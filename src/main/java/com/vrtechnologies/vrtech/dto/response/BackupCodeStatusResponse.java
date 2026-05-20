package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BackupCodeStatusResponse {

    private final long active;
    private final long total;
    private final boolean exists;

    /** Populated only on regeneration; null when simply checking status. */
    private final List<String> generatedCodes;
}
