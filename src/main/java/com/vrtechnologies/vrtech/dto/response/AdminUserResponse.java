package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.AdminStatus;
import com.vrtechnologies.vrtech.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
public class AdminUserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String roleKey;
    private String roleName;
    private AdminStatus status;
    private boolean active;
    private String profileImageUrl;
    private LocalDate accessStartDate;
    private LocalDate accessEndDate;
    private LocalTime allowedLoginStartTime;
    private LocalTime allowedLoginEndTime;
    private List<String> allowedLoginDays;
    private boolean twoFactorEnabled;
    private LocalDateTime lastLoginAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<StoreSummary> stores;

    @Getter
    @Setter
    @Builder
    public static class StoreSummary {
        private Long id;
        private String name;
        private String city;
    }
}
