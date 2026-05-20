package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorVerifyRequest {

    @NotBlank
    private String challengeId;

    @NotBlank
    @Size(min = 4, max = 8)
    @Pattern(regexp = "^\\d{4,8}$", message = "Code must be 4-8 digits")
    private String code;
}
