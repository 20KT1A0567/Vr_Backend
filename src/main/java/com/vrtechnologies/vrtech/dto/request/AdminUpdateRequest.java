package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.Role;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class AdminUpdateRequest {

    private String fullName;

    @Email
    private String email;

    private String phone;

    private Role role;

    private String roleKey;

    private String profileImageUrl;

    private LocalDate accessStartDate;
    private LocalDate accessEndDate;
    private LocalTime allowedLoginStartTime;
    private LocalTime allowedLoginEndTime;

    private List<Long> storeIds;
}
