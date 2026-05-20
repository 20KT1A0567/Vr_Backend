package com.vrtechnologies.vrtech.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationConfigResponse {
    private List<NavigationItemResponse> headerMenu;
    private List<NavigationItemResponse> footerMenu;
    private List<NavigationItemResponse> mobileMenu;
}
