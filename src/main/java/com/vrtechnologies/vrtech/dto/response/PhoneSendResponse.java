package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PhoneSendResponse {
    private String verificationId;
    private String sessionInfo;
    private String token;
}
