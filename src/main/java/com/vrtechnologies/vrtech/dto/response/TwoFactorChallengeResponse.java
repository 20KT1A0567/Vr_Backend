package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TwoFactorChallengeResponse {

    private final boolean twoFactorRequired = true;
    private final String challengeId;
    private final String maskedEmail;
    private final int expiresInSeconds;
    private final int resendCooldownSeconds;
    private final String message;
}
