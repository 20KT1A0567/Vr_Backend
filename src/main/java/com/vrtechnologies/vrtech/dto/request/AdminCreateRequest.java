package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class AdminCreateRequest {

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    private String phone;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @NotNull
    private Role role;

    private String roleKey;

    private String profileImageUrl;

    private LocalDate accessStartDate;
    private LocalDate accessEndDate;
    private LocalTime allowedLoginStartTime;
    private LocalTime allowedLoginEndTime;

    private List<String> allowedLoginDays;

    private Boolean twoFactorEnabled;

    private List<Long> storeIds;
}
