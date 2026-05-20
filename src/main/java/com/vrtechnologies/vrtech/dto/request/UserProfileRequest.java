package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileRequest {
    @NotBlank
    private String name;
    private String email;
    private String phone;
    private String preferredContactName;
    private String preferredContactPhone;
    private String preferredContactEmail;
}
