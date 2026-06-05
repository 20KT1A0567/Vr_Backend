package com.vrtechnologies.vrtech.dto.response;
import com.vrtechnologies.vrtech.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AuthResponse {

    private String token;
    private String refreshToken;
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Role role;
    private String roleKey;
    private String roleName;
    private Long sessionId;
    private LocalDateTime tokenExpiresAt;
    private LocalDateTime refreshTokenExpiresAt;
    private List<String> visibleModules;
    private boolean twoFactorEnabled;
    private String trustedDeviceToken;

    @Singular
    private List<PermissionSummary> permissions;

    @Getter
    @Builder
    public static class PermissionSummary {
        private String module;
        private String action;
    }
}
