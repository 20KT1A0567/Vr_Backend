package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPasswordResetRequest {

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;
}
