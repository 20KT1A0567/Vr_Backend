package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.request.FirebaseLoginRequest;
import com.vrtechnologies.vrtech.dto.request.LoginRequest;
import com.vrtechnologies.vrtech.dto.request.LogoutRequest;
import com.vrtechnologies.vrtech.dto.request.PhoneSendRequest;
import com.vrtechnologies.vrtech.dto.request.PhoneVerifyRequest;
import com.vrtechnologies.vrtech.dto.request.RefreshTokenRequest;
import com.vrtechnologies.vrtech.dto.request.RegisterRequest;
import com.vrtechnologies.vrtech.dto.request.TwoFactorBackupRequest;
import com.vrtechnologies.vrtech.dto.request.TwoFactorResendRequest;
import com.vrtechnologies.vrtech.dto.request.TwoFactorVerifyRequest;
import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.AuthResponse;
import com.vrtechnologies.vrtech.dto.response.LoginOutcome;
import com.vrtechnologies.vrtech.dto.response.PhoneSendResponse;
import com.vrtechnologies.vrtech.dto.response.TwoFactorChallengeResponse;
import com.vrtechnologies.vrtech.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ApiResponse<Object> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        LoginOutcome outcome = authService.login(request, clientIp(http), userAgent(http));
        if (outcome.isTwoFactorRequired()) {
            TwoFactorChallengeResponse challenge = outcome.getTwoFactorChallenge();
            return ApiResponse.ok("Two-factor verification required", challenge);
        }
        return ApiResponse.ok("Login successful", outcome.getAuthResponse());
    }

    @PostMapping("/2fa/verify")
    public ApiResponse<AuthResponse> verifyTwoFactor(@Valid @RequestBody TwoFactorVerifyRequest request) {
        return ApiResponse.ok("Login successful", authService.verifyTwoFactor(request));
    }

    @PostMapping("/2fa/resend")
    public ApiResponse<TwoFactorChallengeResponse> resendTwoFactor(@Valid @RequestBody TwoFactorResendRequest request) {
        return ApiResponse.ok("Verification code resent", authService.resendTwoFactor(request));
    }

    @PostMapping("/2fa/backup")
    public ApiResponse<AuthResponse> verifyBackupCode(@Valid @RequestBody TwoFactorBackupRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Login successful", authService.verifyBackupCode(request, clientIp(http)));
    }

    @PostMapping("/phone/send")
    public ApiResponse<PhoneSendResponse> sendPhoneOtp(@Valid @RequestBody PhoneSendRequest request) {
        return ApiResponse.ok("OTP sent", authService.sendPhoneOtp(request));
    }

    @PostMapping("/phone/verify")
    public ApiResponse<AuthResponse> verifyPhone(@Valid @RequestBody PhoneVerifyRequest request) {
        return ApiResponse.ok("Login successful", authService.verifyPhone(request));
    }

    @PostMapping("/customer/firebase-login")
    public ApiResponse<AuthResponse> firebaseCustomerLogin(@Valid @RequestBody FirebaseLoginRequest request) {
        return ApiResponse.ok("Login successful", authService.firebaseCustomerLogin(request.getFirebaseIdToken()));
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

    @PostMapping("/2fa/totp/setup")
    public ApiResponse<Object> setupTotp() {
        return ApiResponse.ok("TOTP setup generated", authService.setupTotp());
    }

    @PostMapping("/2fa/totp/enable")
    public ApiResponse<Object> enableTotp(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> payload) {
        String code = payload.get("code");
        authService.enableTotp(code);
        return ApiResponse.ok("TOTP multi-factor authentication enabled", null);
    }

    @PostMapping("/2fa/totp/disable")
    public ApiResponse<Object> disableTotp(@org.springframework.web.bind.annotation.RequestBody java.util.Map<String, String> payload) {
        String code = payload.get("code");
        authService.disableTotp(code);
        return ApiResponse.ok("TOTP multi-factor authentication disabled", null);
    }

    private String clientIp(HttpServletRequest http) {
        if (http == null) return null;
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }

    private String userAgent(HttpServletRequest http) {
        if (http == null) return null;
        String value = http.getHeader("User-Agent");
        return value == null ? null : value.length() > 250 ? value.substring(0, 250) : value;
    }
}
