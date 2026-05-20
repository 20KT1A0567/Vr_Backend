package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CouponValidationRequest {
    @NotBlank
    private String code;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal subtotal;
}
