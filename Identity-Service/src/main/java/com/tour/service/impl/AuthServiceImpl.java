package com.tour.service.impl;

import com.tour.dto.request.LoginRequest;
import com.tour.dto.response.LoginResponse;
import com.tour.entity.User;
import com.tour.repository.UserRepository;
import com.tour.service.AuthService;
import com.tour.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

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

    @Override
    public void logout(String token) {
        // Logic: Senior thường sẽ lấy JTI (JWT ID) của token này
        // và lưu vào Redis với TTL bằng thời gian hết hạn còn lại của Token
    }
}
