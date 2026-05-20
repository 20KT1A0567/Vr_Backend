package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String preferredContactName;
    private String preferredContactPhone;
    private String preferredContactEmail;
    private List<UserAddressResponse> addresses;
}
