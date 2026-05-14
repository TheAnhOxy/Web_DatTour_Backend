package com.tour.identity.service.impl;

import com.tour.identity.client.BookingClient;
import com.tour.identity.client.PaymentClient;
import com.tour.identity.dto.event.NotificationEvent;
import com.tour.identity.dto.request.*;
import com.tour.identity.dto.response.ApiResponse;
import com.tour.identity.dto.response.LoginResponse;
import com.tour.identity.dto.response.UserResponse;
import com.tour.identity.dto.response.UserWithBookingResponse;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public
class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final BookingClient bookingClient;
    private final PaymentClient paymentClient;
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
                .token(accessToken)
                .refreshToken(refreshToken)
                .email(user.getEmail())
                .userId(user.getId())
                .build();
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        try {
            // Xác thực refresh token (kiểm tra chữ ký + hết hạn)
            var signedJWT = jwtUtils.verifyToken(request.getRefreshToken());
            String email = signedJWT.getJWTClaimsSet().getSubject();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

            // Cấp cặp token mới
            String newAccessToken  = jwtUtils.generateAccessToken(user);
            String newRefreshToken = jwtUtils.generateRefreshToken(user);

            log.info("Token refreshed for: {}", email);

            return LoginResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .email(user.getEmail())
                    .userId(user.getId())
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("REFRESH_TOKEN_INVALID");
        }
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

        // Sinh OTP 6 số và lưu Redis (TTL 10 phút)
        String otp = String.format("%06d", (int) (Math.random() * 1_000_000));
        redisTemplate.opsForValue().set(OTP_PREFIX + user.getEmail(), otp, 10, TimeUnit.MINUTES);

        // Gửi email chào mừng kèm OTP qua Kafka
        NotificationEvent event = NotificationEvent.builder()
                .channel("EMAIL")
                .recipient(user.getEmail())
                .templateCode("REGISTRATION_OTP")
                .param(Map.of("name", user.getFullName(), "otp", otp))
                .build();

        kafkaTemplate.send("notification-topic", event);
        log.info("Registration OTP sent for: {}", user.getEmail());
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

    private static final String VERIFIED_PREFIX = "VERIFIED:";

    @Override
    public void verifyOtp(VerifyOtpRequest request) {
        String storedOtp = redisTemplate.opsForValue().get(OTP_PREFIX + request.getEmail());

        if (storedOtp == null) throw new RuntimeException("OTP_EXPIRED");
        if (!storedOtp.equals(request.getOtp())) throw new RuntimeException("INVALID_OTP");
        redisTemplate.delete(OTP_PREFIX + request.getEmail());

        // Tạo một "vé thông hành" trong Redis để cho phép reset password
        // Key: VERIFIED:theanh@gmail.com, Value: true, TTL: 10 phút
        redisTemplate.opsForValue().set(VERIFIED_PREFIX + request.getEmail(), "true", 10, TimeUnit.MINUTES);

        log.info("OTP verified for: {}", request.getEmail());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String isVerified = redisTemplate.opsForValue().get(VERIFIED_PREFIX + request.getEmail());

        if (isVerified == null || !isVerified.equals("true")) {
            throw new RuntimeException("PLEASE_VERIFY_OTP_FIRST");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTemplate.delete(VERIFIED_PREFIX + request.getEmail());
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

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        try {
            var signedJWT = jwtUtils.verifyToken(request.getToken());
            String jti = signedJWT.getJWTClaimsSet().getJWTID();

            // Check trong Blacklist Redis
            boolean isBlacklisted = redisTemplate.hasKey("BLACKLIST:" + jti);

            return IntrospectResponse.builder()
                    .valid(!isBlacklisted)
                    .build();
        } catch (Exception e) {
            return IntrospectResponse.builder().valid(false).build();
        }
    }

    @Override
    public List<UserResponse> getAllUsers() {
        // Sau khi sửa ở Repository, 'user' ở đây sẽ được hiểu là kiểu User entity
        return userRepository.findAllByStatusNot("DELETED").stream()
                .map(user -> UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .phone(user.getPhone())
                        .address(user.getAddress())
                        .dob(user.getDob())
                        .gender(user.getGender())
                        .status(user.getStatus())
                        .avatarUrl(user.getAvatarUrl())
                        .currentPoints(user.getCurrentPoints())
                        .roles(user.getRoles().stream()
                                .map(role -> role.getName()) // Lấy tên Role (ADMIN, USER...)
                                .collect(Collectors.toSet()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));
        user.setStatus("DELETED");
        userRepository.save(user);

        log.info("User with ID {} has been soft-deleted", userId);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        // Chặn luôn nếu user đã bị xóa (soft-delete) nếu tiền bối muốn
        if ("DELETED".equals(user.getStatus())) {
            throw new RuntimeException("USER_HAS_BEEN_DELETED");
        }

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .dob(user.getDob())
                .gender(user.getGender())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .currentPoints(user.getCurrentPoints())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    @Transactional
    public UserResponse adminUpdateUser(Long id, UpdateProfileRequest request) {
        // 1. Tìm user
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("USER_NOT_FOUND"));

        if ("DELETED".equals(user.getStatus())) {
            throw new RuntimeException("CANNOT_UPDATE_DELETED_USER");
        }

        // 2. Cập nhật thông tin (Chỉ cập nhật những gì request có gửi)
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getDob() != null) user.setDob(request.getDob());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        // Admin có thể cập nhật thêm trạng thái nếu tiền bối muốn bổ sung vào DTO
        // if (request.getStatus() != null) user.setStatus(request.getStatus());

        // 3. Lưu vào DB
        User updatedUser = userRepository.save(user);

        // 4. Trả về UserResponse (Tương tự getUserById)
        return UserResponse.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .fullName(updatedUser.getFullName())
                .phone(updatedUser.getPhone())
                .address(updatedUser.getAddress())
                .dob(updatedUser.getDob())
                .gender(updatedUser.getGender())
                .status(updatedUser.getStatus())
                .avatarUrl(updatedUser.getAvatarUrl())
                .currentPoints(updatedUser.getCurrentPoints())
                .roles(updatedUser.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    public List<UserWithBookingResponse> getAllUsersWithBookings() {
        List<UserResponse> users = getAllUsers();
        List<Long> userIds = users.stream().map(UserResponse::getId).toList();

        ApiResponse bookingRes = bookingClient.getBookingsByUserIds(userIds);

        // Debug: log ra để xem data thực tế từ Booking Service gửi sang là gì
        log.info("=> Data từ Booking Service: {}", bookingRes.getData());

        // Jackson luôn coi Key là String khi deserialize vào Map
        Map<String, Object> bookingMap = (Map<String, Object>) bookingRes.getData();

        return users.stream().map(user -> {
            String userIdStr = String.valueOf(user.getId());

            // Lấy list booking, nếu null thì trả về mảng rỗng
            Object bookingsObj = (bookingMap != null) ? bookingMap.get(userIdStr) : null;
            List<Object> userBookings = (bookingsObj instanceof List) ? (List<Object>) bookingsObj : List.of();

            return UserWithBookingResponse.builder()
                    .user(user)
                    .bookings(userBookings)
                    .build();
        }).toList();
    }
    @Override
    public List<Map<String, Object>> getFullPaymentReport() {
        // 1. Lấy danh sách Payment từ Payment Service
        ApiResponse paymentRes = paymentClient.getAllPayments();
        List<Map<String, Object>> payments = (List<Map<String, Object>>) paymentRes.getData();
        if (payments == null || payments.isEmpty()) return List.of();

        // 2. Lấy Map Bookings từ Booking Service (Tiền bối đã làm rất tốt bước này)
        List<Long> bookingIds = payments.stream()
                .map(p -> Long.valueOf(p.get("bookingId").toString()))
                .distinct().toList();
        ApiResponse bookingRes = bookingClient.getBookingsByIds(bookingIds);
        Map<String, Object> bookingMap = (Map<String, Object>) bookingRes.getData();

        // 3. Gộp dữ liệu
        return payments.stream().map(payment -> {
            String bIdStr = payment.get("bookingId").toString();
            Object bInfo = bookingMap != null ? bookingMap.get(bIdStr) : null;
            payment.put("bookingDetails", bInfo);

            // --- ĐẮP THỊT CUSTOMER AN TOÀN ---
            if (bInfo instanceof Map) {
                Map<String, Object> bInfoMap = (Map<String, Object>) bInfo;
                Object userIdObj = bInfoMap.get("userId");

                if (userIdObj != null) {
                    Long userId = Long.valueOf(userIdObj.toString());

                    // SỬA TẠI ĐÂY: Dùng findById để tránh văng Exception USER_NOT_FOUND
                    userRepository.findById(userId).ifPresentOrElse(
                            user -> payment.put("customerDetails", mapToUserResponse(user)), // Hàm map user của tiền bối
                            () -> payment.put("customerDetails", "Khách hàng không tồn tại (ID: " + userId + ")")
                    );
                }
            }
            return payment;
        }).toList();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .address(user.getAddress())
                .dob(user.getDob())
                .gender(user.getGender())
                .status(user.getStatus())
                .currentPoints(user.getCurrentPoints())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName())
                        .collect(Collectors.toSet()))
                .build();
    }
}
