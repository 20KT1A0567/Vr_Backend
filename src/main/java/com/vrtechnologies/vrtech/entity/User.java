package com.vrtechnologies.vrtech.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.vrtechnologies.vrtech.entity.enums.AdminStatus;
import com.vrtechnologies.vrtech.entity.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(unique = true, length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(name = "admin_role_key", length = 64)
    private String adminRoleKey;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "preferred_contact_name")
    private String preferredContactName;

    @Column(name = "preferred_contact_phone")
    private String preferredContactPhone;

    @Column(name = "preferred_contact_email")
    private String preferredContactEmail;

    @Column(name = "preferred_delivery_address", columnDefinition = "TEXT")
    private String preferredDeliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status")
    private AdminStatus adminStatus;

    @Column(name = "access_start_date")
    private LocalDate accessStartDate;

    @Column(name = "access_end_date")
    private LocalDate accessEndDate;

    @Column(name = "allowed_login_start_time")
    private LocalTime allowedLoginStartTime;

    @Column(name = "allowed_login_end_time")
    private LocalTime allowedLoginEndTime;

    @Column(name = "allowed_login_days", length = 32)
    private String allowedLoginDays;

    @Column(name = "two_factor_enabled", nullable = false)
    private boolean twoFactorEnabled = false;

    @Column(name = "totp_secret", length = 32)
    private String totpSecret;

    @JsonIgnore
    public boolean isTotpEnabled() {
        return twoFactorEnabled && totpSecret != null && !totpSecret.isBlank();
    }

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "created_by")
    private Long createdBy;

    @JsonIgnore
    public boolean isAdmin() {
        return role != Role.USER;
    }

    @JsonIgnore
    public boolean isSuperAdmin() {
        return role == Role.SUPER_ADMIN;
    }

    @JsonIgnore
    public boolean isLegacyAdmin() {
        return role == Role.ADMIN;
    }

    @JsonIgnore
    public AdminStatus effectiveAdminStatus() {
        if (adminStatus != null) {
            return adminStatus;
        }
        return active ? AdminStatus.ACTIVE : AdminStatus.DISABLED;
    }

    @JsonIgnore
    public EnumSet<DayOfWeek> allowedLoginDaysSet() {
        EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);
        if (allowedLoginDays == null || allowedLoginDays.isBlank()) {
            return result;
        }
        for (String token : allowedLoginDays.split(",")) {
            String trimmed = token.trim().toUpperCase(Locale.ROOT);
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(DayOfWeek.valueOf(trimmed));
            } catch (IllegalArgumentException ignored) {
                // Tolerate legacy / malformed values
            }
        }
        return result;
    }

    public void setAllowedLoginDaysFromSet(Set<DayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            this.allowedLoginDays = null;
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (DayOfWeek day : days) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(day.name());
        }
        this.allowedLoginDays = sb.toString();
    }
}
