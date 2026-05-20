package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RazorpaySettingsResponse {

    private boolean enabled;
    private boolean configured;
    private String keyId;
    private boolean keySecretConfigured;
    private boolean webhookSecretConfigured;
    private String currency;
    private String merchantName;
    private String apiBaseUrl;
}
