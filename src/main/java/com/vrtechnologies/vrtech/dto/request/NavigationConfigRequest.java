package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NavigationConfigRequest {
    @Valid
    @NotNull
    private List<NavigationItemRequest> items;
}
