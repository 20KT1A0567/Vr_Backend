package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRoleCreateRequest {

    @NotBlank
    private String roleKey;

    @NotBlank
    private String displayName;

    private String description;

    @NotNull
    private Role baseRole;
}
