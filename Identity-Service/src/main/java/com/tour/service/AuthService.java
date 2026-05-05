package com.tour.service;

import com.tour.dto.request.LoginRequest;
import com.tour.dto.request.RegisterRequest;
import com.tour.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
    void logout(String token);
    void register(RegisterRequest request);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
}
