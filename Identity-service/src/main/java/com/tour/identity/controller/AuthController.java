package com.tour.identity.controller;

import com.tour.identity.dto.request.*;
import com.tour.identity.dto.response.ApiResponse;
import com.tour.identity.dto.response.LoginResponse;
import com.tour.identity.service.AuthService;
import com.tour.identity.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RoleService roleService;

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

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("OTP verified successfully. You can now reset your password.")
                .build());
    }

    // API này bây giờ không nhận OTP nữa
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

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Token refreshed successfully")
                .data(response)
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

    @GetMapping("/profile/{id}")
    public ResponseEntity<ApiResponse> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Get profile successfully")
                .data(authService.getUserById(id))
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

    // API nội bộ cho Gateway
    @PostMapping("/introspect")
    public ResponseEntity<ApiResponse> introspect(@RequestBody IntrospectRequest request) {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .data(authService.introspect(request))
                .build());
    }


    @PostMapping("/admin/assign-roles")
    // @PreAuthorize("hasRole('ADMIN')") // Spring Security Method Security
    public ResponseEntity<ApiResponse> assignRoles(@RequestBody RoleAssignmentRequest request) {
        roleService.assignRoles(request);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Roles assigned successfully")
                .build());
    }

    // 3. Soft Delete User
    @DeleteMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("User deleted successfully (Soft-delete)")
                .build());
    }

    @PutMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse> adminUpdateUser(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("User updated successfully by Admin")
                .data(authService.adminUpdateUser(id, request))
                .build());
    }

    @GetMapping("/admin/users")
    public ResponseEntity<ApiResponse> getAllUsers(){
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .data(authService.getAllUsers())
                .build());
    }


    @GetMapping("/admin/users/{id}")
    public ResponseEntity<ApiResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Get user successfully")
                .data(authService.getUserById(id))
                .build());
    }
    @GetMapping("/admin/users-with-bookings")
    public ResponseEntity<ApiResponse> getAllUsersWithBookings() {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách User và lịch sử đặt tour thành công")
                .data(authService.getAllUsersWithBookings())
                .build());
    }

    @GetMapping("/admin/payment-report")
    public ResponseEntity<ApiResponse> getFullReport() {
        return ResponseEntity.ok(ApiResponse.builder()
                .status(200)
                .message("Báo cáo tổng hợp thành công")
                .data(authService.getFullPaymentReport())
                .build());
    }


}