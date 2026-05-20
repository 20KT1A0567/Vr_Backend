package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PriceDropAlertRequest {

    @NotNull
    private Long productId;

    @Email
    @NotBlank
    private String email;

    private String phone;
    private BigDecimal targetPrice;
}
