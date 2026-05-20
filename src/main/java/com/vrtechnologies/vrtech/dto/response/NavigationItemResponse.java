package com.vrtechnologies.vrtech.dto.response;

import com.vrtechnologies.vrtech.entity.enums.NavigationMenuLocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationItemResponse {
    private Long id;
    private NavigationMenuLocation menuLocation;
    private String label;
    private String url;
    private boolean visible;
    private Integer sortOrder;
}
