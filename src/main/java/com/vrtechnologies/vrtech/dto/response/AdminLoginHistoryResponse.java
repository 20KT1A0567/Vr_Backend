package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminLoginHistoryResponse {

    private Long id;
    private Long adminId;
    private String adminEmail;
    private Long sessionId;
    private LocalDateTime loginAt;
    private LocalDateTime logoutAt;
    private String ipAddress;
    private String userAgent;
    private String status;
    private String failureReason;
}
