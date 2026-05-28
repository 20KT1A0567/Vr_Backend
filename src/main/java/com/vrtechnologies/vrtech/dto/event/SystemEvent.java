package com.vrtechnologies.vrtech.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemEvent {
    private String eventType; // e.g. "ORDER_CREATED", "STOCK_ALERT", "SECURITY_ALERT", "SETTING_MUTATED"
    private String title;
    private String message;
    private String severity; // INFO, WARNING, CRITICAL
    private Object payload;
    private LocalDateTime timestamp;
}
