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
public class PermissionCatalogResponse {

    private List<Module> modules;
    private List<PermissionAction> actions;
    private List<Role> roles;
    private List<AdminRoleResponse> managedRoles;
}
