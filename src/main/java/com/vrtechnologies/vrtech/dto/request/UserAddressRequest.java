package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAddressRequest {
    @NotBlank
    private String label;
    @NotBlank
    private String contactName;
    @NotBlank
    private String contactPhone;
    private String contactEmail;
    @NotBlank
    private String address;
    private String city;
    private String state;
    private String postalCode;
    private boolean defaultAddress;
}
