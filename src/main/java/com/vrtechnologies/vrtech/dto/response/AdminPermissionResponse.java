package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.Module;
import com.vrtechnologies.vrtech.entity.enums.PermissionAction;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class AdminPermissionResponse {

    private Long adminId;
    private List<Entry> entries;
    private List<Module> visibleModules;

    @Getter
    @Setter
    @Builder
    public static class Entry {
        private Module module;
        private PermissionAction action;
        private boolean granted;
        private boolean fromOverride;
    }
}
