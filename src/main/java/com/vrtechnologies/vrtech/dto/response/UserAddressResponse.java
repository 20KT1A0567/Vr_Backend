package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserAddressResponse {
    private Long id;
    private String label;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private boolean defaultAddress;
}
