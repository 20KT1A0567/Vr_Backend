package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogEntryResponse {

    private final Long id;
    private final Long adminId;
    private final String adminEmail;
    private final String module;
    private final String action;
    private final String entityType;
    private final Long entityId;
    private final String oldValue;
    private final String newValue;
    private final String description;
    private final String ipAddress;
    private final LocalDateTime createdAt;
}
