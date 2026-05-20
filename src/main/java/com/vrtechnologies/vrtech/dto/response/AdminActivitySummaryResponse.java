package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminActivitySummaryResponse {
    private long todayChanges;
    private long failedLoginsToday;
    private long suspiciousActionsToday;
    private long openReturns;
    private long failedPayments;
}
