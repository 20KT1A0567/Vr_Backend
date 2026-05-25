package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;

@Getter
@Builder
public class CheckoutProfileResponse {

    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String defaultDeliveryAddress;

    @Singular
    private List<SavedAddressResponse> savedAddresses;

    @Getter
    @Builder
    public static class SavedAddressResponse {
        private String id;
        private String label;
        private String address;
        private String contactName;
        private String contactPhone;
        private String state;
        private String postalCode;
        private boolean defaultAddress;
    }
}
