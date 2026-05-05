package com.tour.identity.controller;

import com.tour.identity.dto.request.LoginRequest;
import com.tour.identity.dto.request.RegisterRequest;
import com.tour.identity.dto.request.ResetPasswordRequest;
import com.tour.identity.dto.request.UpdateProfileRequest;
import com.tour.identity.dto.response.ApiResponse;
import com.tour.identity.dto.response.LoginResponse;
import com.tour.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Login successfully")
                .data(response)
                .build());
    }



    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(201).body(ApiResponse.builder()
                .status(201)
                .message("Register successfully. Please check your email.")
                .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Password has been reset successfully")
                .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("OTP has been sent to your email")
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Logged out successfully")
                .build());
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<ApiResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        authService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Profile updated successfully")
                .build());
    }


}