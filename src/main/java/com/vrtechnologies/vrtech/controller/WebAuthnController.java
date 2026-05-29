package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.AuthResponse;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.entity.WebAuthnCredential;
import com.vrtechnologies.vrtech.service.AuthService;
import com.vrtechnologies.vrtech.service.UserContextService;
import com.vrtechnologies.vrtech.service.WebAuthnService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class WebAuthnController {

    private final WebAuthnService webAuthnService;
    private final AuthService authService;
    private final UserContextService userContextService;

    public WebAuthnController(WebAuthnService webAuthnService,
                              AuthService authService,
                              UserContextService userContextService) {
        this.webAuthnService = webAuthnService;
        this.authService = authService;
        this.userContextService = userContextService;
    }

    // ──────────────────────────── REGISTRATION (authenticated) ─────────────────────

    @PostMapping("/api/auth/webauthn/register/begin")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> registerBegin() throws Exception {
        User user = userContextService.getCurrentUser();
        String json = webAuthnService.startRegistration(user);
        return ApiResponse.ok("Registration options generated", json);
    }

    @PostMapping("/api/auth/webauthn/register/finish")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Object> registerFinish(@RequestBody Map<String, String> body) throws Exception {
        User user = userContextService.getCurrentUser();
        String sessionKey = body.get("sessionKey");
        String credential = body.get("credential");
        String nickname   = body.getOrDefault("nickname", "Passkey");
        webAuthnService.finishRegistration(user, sessionKey, credential, nickname);
        return ApiResponse.ok("Passkey registered successfully", null);
    }

    // ──────────────────────────── MY PASSKEYS (authenticated) ─────────────────────

    @GetMapping("/api/admin/me/passkeys")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<PasskeyResponse>> listPasskeys() {
        User user = userContextService.getCurrentUser();
        List<PasskeyResponse> passkeys = webAuthnService.listCredentials(user.getId())
                .stream()
                .map(c -> new PasskeyResponse(
                        c.getId(),
                        c.getNickname(),
                        c.getCreatedAt() != null ? c.getCreatedAt().toString() : null,
                        c.getLastUsedAt() != null ? c.getLastUsedAt().toString() : null
                ))
                .toList();
        return ApiResponse.ok("Passkeys", passkeys);
    }

    @DeleteMapping("/api/admin/me/passkeys/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Object> deletePasskey(@PathVariable Long id) {
        User user = userContextService.getCurrentUser();
        webAuthnService.deleteCredential(id, user.getId());
        return ApiResponse.ok("Passkey removed", null);
    }

    // ──────────────────────────── AUTHENTICATION (public) ─────────────────────────

    @PostMapping("/api/auth/webauthn/authenticate/begin")
    public ApiResponse<String> authenticateBegin() throws Exception {
        String json = webAuthnService.startAuthentication();
        return ApiResponse.ok("Authentication options generated", json);
    }

    @PostMapping("/api/auth/webauthn/authenticate/finish")
    public ApiResponse<AuthResponse> authenticateFinish(@RequestBody Map<String, String> body,
                                                         HttpServletRequest request) throws Exception {
        String sessionKey = body.get("sessionKey");
        String assertion  = body.get("credential");
        User user = webAuthnService.finishAuthentication(sessionKey, assertion);
        AuthResponse authResponse = authService.loginAsUser(user, clientIp(request));
        return ApiResponse.ok("Login successful", authResponse);
    }

    private String clientIp(HttpServletRequest http) {
        if (http == null) return null;
        String forwarded = http.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return http.getRemoteAddr();
    }

    // ─── Inner DTO ─────────────────────────────────────────────────────────────────

    public record PasskeyResponse(Long id, String nickname, String createdAt, String lastUsedAt) {}
}
