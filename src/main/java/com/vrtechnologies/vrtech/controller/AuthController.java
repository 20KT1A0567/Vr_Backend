package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.LoginRequest;
import com.vrtechnologies.vrtech.dto.request.LogoutRequest;
import com.vrtechnologies.vrtech.dto.request.PhoneVerifyRequest;
import com.vrtechnologies.vrtech.dto.request.RefreshTokenRequest;
import com.vrtechnologies.vrtech.dto.request.RegisterRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.AuthResponse;
import com.vrtechnologies.vrtech.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok("Account created", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/phone/verify")
    public ApiResponse<AuthResponse> verifyPhone(@Valid @RequestBody PhoneVerifyRequest request) {
        return ApiResponse.ok("Login successful", authService.verifyPhone(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok("Token refreshed", authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Object> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.ok("Logout successful", null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse> me() {
        return ApiResponse.ok("Current user", authService.me());
    }
}
