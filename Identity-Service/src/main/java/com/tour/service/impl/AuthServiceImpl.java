package com.tour.service.impl;

import com.tour.dto.event.NotificationEvent;
import com.tour.dto.request.LoginRequest;
import com.tour.dto.request.RegisterRequest;
import com.tour.dto.request.ResetPasswordRequest;
import com.tour.dto.response.LoginResponse;
import com.tour.entity.User;
import com.tour.repository.UserRepository;
import com.tour.service.AuthService;
import com.tour.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // 1. Lấy OTP từ Redis
        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + request.getEmail());

        if (storedOtp == null) throw new RuntimeException("OTP_EXPIRED");
        if (!storedOtp.equals(request.getOtp())) throw new RuntimeException("INVALID_OTP");

        // 2. Tìm user và update mật khẩu
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // 3. Xóa OTP sau khi dùng xong
        redisTemplate.delete(OTP_PREFIX + request.getEmail());
        log.info("Password reset successfully for: {}", request.getEmail());
    }

    @Override
    public void logout(String token) {
        // Logic: Senior thường sẽ lấy JTI (JWT ID) của token này
        // và lưu vào Redis với TTL bằng thời gian hết hạn còn lại của Token
    }
}
