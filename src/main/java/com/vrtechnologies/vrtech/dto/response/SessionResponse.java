package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SessionResponse {

    private final Long id;
    private final String ipAddress;
    private final String userAgent;
    private final LocalDateTime lastUsedAt;
    private final LocalDateTime createdAt;
    private final LocalDateTime refreshTokenExpiresAt;
    private final boolean current;
}
