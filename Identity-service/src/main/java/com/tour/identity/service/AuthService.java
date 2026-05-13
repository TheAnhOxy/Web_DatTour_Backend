package com.tour.identity.service;

import com.tour.identity.dto.request.*;
import com.tour.identity.dto.response.LoginResponse;
import com.tour.identity.dto.response.UserResponse;
import com.tour.identity.dto.response.UserWithBookingResponse;

import java.util.List;
import java.util.Map;

public interface AuthService {

    LoginResponse login(LoginRequest request);
    LoginResponse refreshToken(RefreshTokenRequest request);
    void logout(String token);
    void register(RegisterRequest request);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    void updateProfile(Long userId, UpdateProfileRequest request);
    IntrospectResponse introspect(IntrospectRequest request);
    void deleteUser(Long userId);
    List<UserResponse> getAllUsers();
    void verifyOtp(VerifyOtpRequest request);
    UserResponse getUserById(Long id);
    UserResponse adminUpdateUser(Long id, UpdateProfileRequest request);;
    List<UserWithBookingResponse> getAllUsersWithBookings();
    List<Map<String, Object>> getFullPaymentReport();
}
