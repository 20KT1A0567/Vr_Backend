package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecentProductViewRequest {

    @NotNull
    private Long productId;

    private String anonymousId;
}
