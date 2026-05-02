package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import com.vrtechnologies.vrtech.entity.enums.Role;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class RolePermissionsResponse {

    private Role role;
    private String roleKey;
    private String displayName;
    private String description;
    private boolean active;
    private boolean protectedRole;
    private boolean systemRole;
    private long adminCount;
    private List<Entry> entries;

    @Getter
    @Setter
    @Builder
    public static class Entry {
        private Module module;
        private PermissionAction action;
        private boolean granted;
    }
}
