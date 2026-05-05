package com.tour.identity.service;

import com.tour.identity.dto.request.LoginRequest;
import com.tour.identity.dto.request.RegisterRequest;
import com.tour.identity.dto.request.ResetPasswordRequest;
import com.tour.identity.dto.request.UpdateProfileRequest;
import com.tour.identity.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
    void logout(String token);
    void register(RegisterRequest request);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
    void updateProfile(Long userId, UpdateProfileRequest request);
}
