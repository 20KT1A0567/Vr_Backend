package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdminRoleResponse {

    private String roleKey;
    private String displayName;
    private String description;
    private Role baseRole;
    private boolean active;
    private boolean protectedRole;
    private boolean systemRole;
    private long adminCount;
}
