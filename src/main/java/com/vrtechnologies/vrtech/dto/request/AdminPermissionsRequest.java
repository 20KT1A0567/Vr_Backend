package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminPermissionsRequest {

    private List<Entry> permissions;

    @Getter
    @Setter
    public static class Entry {
        private Module module;
        private PermissionAction action;
        private boolean granted = true;
    }
}
