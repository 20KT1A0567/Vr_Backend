package com.vrtechnologies.vrtech.controller;

import com.vrtechnologies.vrtech.dto.response.ApiResponse;
import com.vrtechnologies.vrtech.dto.response.BackupCodeStatusResponse;
import com.vrtechnologies.vrtech.dto.response.SessionResponse;
import com.vrtechnologies.vrtech.entity.AuthSession;
import com.vrtechnologies.vrtech.entity.User;
import com.vrtechnologies.vrtech.exception.BadRequestException;
import com.vrtechnologies.vrtech.service.AdminBackupCodeService;
import com.vrtechnologies.vrtech.service.AuthSessionService;
import com.vrtechnologies.vrtech.service.UserContextService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin/me")
@PreAuthorize("isAuthenticated()")
public class AccountSecurityController {

    private final AuthSessionService authSessionService;
    private final AdminBackupCodeService backupCodeService;
    private final UserContextService userContextService;

    public AccountSecurityController(
            AuthSessionService authSessionService,
            AdminBackupCodeService backupCodeService,
            UserContextService userContextService
    ) {
        this.authSessionService = authSessionService;
        this.backupCodeService = backupCodeService;
        this.userContextService = userContextService;
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionResponse>> listSessions(HttpServletRequest request) {
        User me = userContextService.getCurrentUser();
        Long currentSessionId = currentSessionId(request);
        List<AuthSession> sessions = authSessionService.listActiveForUser(me.getId());
        List<SessionResponse> result = sessions.stream()
                .map(session -> SessionResponse.builder()
                        .id(session.getId())
                        .ipAddress(session.getIpAddress())
                        .userAgent(session.getUserAgent())
                        .lastUsedAt(session.getLastUsedAt())
                        .createdAt(session.getCreatedAt())
                        .refreshTokenExpiresAt(session.getRefreshTokenExpiresAt())
                        .current(currentSessionId != null && Objects.equals(currentSessionId, session.getId()))
                        .build())
                .toList();
        return ApiResponse.ok("Active sessions", result);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Object> revokeSession(@PathVariable Long sessionId, HttpServletRequest request) {
        User me = userContextService.getCurrentUser();
        Long currentSessionId = currentSessionId(request);
        if (currentSessionId != null && Objects.equals(currentSessionId, sessionId)) {
            throw new BadRequestException("Use /logout to end your current session");
        }
        authSessionService.revokeById(me.getId(), sessionId);
        return ApiResponse.ok("Session revoked", null);
    }

    @PostMapping("/sessions/revoke-others")
    public ApiResponse<Object> revokeOthers(HttpServletRequest request) {
        User me = userContextService.getCurrentUser();
        Long currentSessionId = currentSessionId(request);
        int revoked = authSessionService.revokeAllExcept(me.getId(), currentSessionId);
        return ApiResponse.ok(revoked + " session(s) revoked", null);
    }

    @GetMapping("/backup-codes")
    public ApiResponse<BackupCodeStatusResponse> backupCodeStatus() {
        User me = userContextService.getCurrentUser();
        AdminBackupCodeService.BackupCodeStatus status = backupCodeService.status(me);
        return ApiResponse.ok("Backup code status", BackupCodeStatusResponse.builder()
                .active(status.active())
                .total(status.total())
                .exists(status.exists())
                .build());
    }

    @PostMapping("/backup-codes/regenerate")
    public ApiResponse<BackupCodeStatusResponse> regenerateBackupCodes() {
        User me = userContextService.getCurrentUser();
        List<String> codes = backupCodeService.regenerate(me);
        AdminBackupCodeService.BackupCodeStatus status = backupCodeService.status(me);
        return ApiResponse.ok("Backup codes generated. Save them now — you will not see them again.",
                BackupCodeStatusResponse.builder()
                        .active(status.active())
                        .total(status.total())
                        .exists(true)
                        .generatedCodes(codes)
                        .build());
    }

    private Long currentSessionId(HttpServletRequest request) {
        Object value = request.getAttribute("currentSessionId");
        return value instanceof Long ? (Long) value : null;
    }
}
