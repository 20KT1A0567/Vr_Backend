package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorBackupRequest {

    @NotBlank
    private String challengeId;

    @NotBlank
    @Size(min = 6, max = 32)
    private String backupCode;
}
