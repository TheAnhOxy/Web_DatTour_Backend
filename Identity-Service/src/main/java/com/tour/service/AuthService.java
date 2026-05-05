package com.tour.service;

import com.tour.dto.request.LoginRequest;
import com.tour.dto.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);
    void logout(String token);

}
