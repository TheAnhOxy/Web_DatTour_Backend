package com.tour.identity.service.impl;

import com.tour.identity.dto.event.NotificationEvent;
import com.tour.identity.dto.request.LoginRequest;
import com.tour.identity.dto.request.RegisterRequest;
import com.tour.identity.dto.request.ResetPasswordRequest;
import com.tour.identity.dto.request.UpdateProfileRequest;
import com.tour.identity.dto.response.LoginResponse;
import com.tour.identity.entity.User;
import com.tour.identity.repository.UserRepository;
import com.tour.identity.service.AuthService;
import com.tour.identity.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Password incorrect");
        }

        // Tạo cặp Token
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        return LoginResponse.builder()
                .token(accessToken)          // Access Token
                .refreshToken(refreshToken) // Refresh Token
                .email(user.getEmail())
                .userId(user.getId())
                .build();
    }




    private static final String OTP_PREFIX = "OTP:";

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail()))
            throw new RuntimeException("EMAIL_ALREADY_EXISTS");

        if (userRepository.existsByPhone(request.getPhone()))
            throw new RuntimeException("PHONE_ALREADY_EXISTS");

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .dob(request.getDob())
                .gender(request.getGender())
                .status("ACTIVE")
                .emailVerified(false)
                .currentPoints(0)
                .build();

        userRepository.save(user);

        // Bắn event chào mừng qua Kafka
        NotificationEvent event = NotificationEvent.builder()
                .channel("EMAIL")
                .recipient(user.getEmail())
                .templateCode("WELCOME_EMAIL")
                .param(Map.of("name", user.getFullName()))
                .build();

        kafkaTemplate.send("notification-topic", event);
        log.info("Registration event sent for: {}", user.getEmail());
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        // Generate 6-digit OTP
        String otp = String.format("%06d", (int) (Math.random() * 1000000));

        // Lưu vào Redis (TTL 5 phút)
        redisTemplate.opsForValue().set(OTP_PREFIX + email, otp, 5, TimeUnit.MINUTES);

        // Bắn event gửi OTP qua Kafka
        NotificationEvent event = NotificationEvent.builder()
                .channel("EMAIL")
                .recipient(email)
                .templateCode("FORGOT_PASSWORD")
                .param(Map.of("otp", otp, "name", user.getFullName()))
                .build();

        kafkaTemplate.send("notification-topic", event);
        log.info("Forgot password OTP sent to Kafka for: {}", email);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // Lấy OTP từ Redis
        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + request.getEmail());

        if (storedOtp == null) throw new RuntimeException("OTP_EXPIRED");
        if (!storedOtp.equals(request.getOtp())) throw new RuntimeException("INVALID_OTP");
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Xóa OTP sau khi dùng xong
        redisTemplate.delete(OTP_PREFIX + request.getEmail());
        log.info("Password reset successfully for: {}", request.getEmail());
    }

    @Override
    public void logout(String token) {
        try {
            // Verify và lấy object SignedJWT
            var signedJWT = jwtUtils.verifyToken(token);
            // Lấy JTI (ID duy nhất của token)
            String jti = signedJWT.getJWTClaimsSet().getJWTID();

            //  Lấy thời gian hết hạn
            Date expiryTime = signedJWT.getJWTClaimsSet().getExpirationTime();

            // Tính toán TTL cho Redis
            long ttlInSeconds = (expiryTime.getTime() - System.currentTimeMillis()) / 1000;

            if (ttlInSeconds > 0) {
                redisTemplate.opsForValue().set(
                        "BLACKLIST:" + jti,
                        "logged_out",
                        ttlInSeconds,
                        TimeUnit.SECONDS
                );
                log.info("Successfully blacklisted JTI: {}", jti);
            }
        } catch (Exception e) {
            log.warn("Logout attempt with invalid/expired token: {}", e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
    }
}
