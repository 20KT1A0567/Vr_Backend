package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StoreAvailabilityResponse {
    private Long storeId;
    private String storeName;
    private String city;
    private Integer stockQuantity;
    private boolean available;
}
