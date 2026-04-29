package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteSettingsRequest {

    @NotBlank
    private String companyName;

    private String supportEmail;

    private String supportPhone;
    private String shippingNote;
    private String returnPolicy;
    private String defaultCity;
    private String defaultState;
    private String mapLink;
}
