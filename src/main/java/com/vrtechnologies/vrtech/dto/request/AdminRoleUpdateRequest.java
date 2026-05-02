package com.vrtechnologies.vrtech.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminRoleUpdateRequest {

    private String displayName;
    private String description;
    private Boolean active;
}
