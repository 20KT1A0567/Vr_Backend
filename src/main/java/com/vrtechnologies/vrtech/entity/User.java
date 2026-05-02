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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

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

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

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

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

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
}
