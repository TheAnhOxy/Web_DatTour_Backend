package com.tour.identity.service;

import com.tour.identity.dto.request.*;
import com.tour.identity.dto.response.LoginResponse;
import com.tour.identity.dto.response.UserResponse;

import java.util.List;

public interface AuthService {

    LoginResponse login(LoginRequest request);
    void logout(String token);
    void register(RegisterRequest request);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    void updateProfile(Long userId, UpdateProfileRequest request);
    IntrospectResponse introspect(IntrospectRequest request);
    void deleteUser(Long userId);
    List<UserResponse> getAllUsers();
}
