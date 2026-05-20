package com.vrtechnologies.vrtech.dto.request;

import com.vrtechnologies.vrtech.entity.enums.NavigationMenuLocation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NavigationItemRequest {
    private Long id;

    @NotNull
    private NavigationMenuLocation menuLocation;

    @NotBlank
    private String label;

    @NotBlank
    private String url;

    private Boolean visible;
    private Integer sortOrder;
}
