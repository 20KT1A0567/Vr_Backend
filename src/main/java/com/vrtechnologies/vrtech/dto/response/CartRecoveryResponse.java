package com.vrtechnologies.vrtech.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CartRecoveryResponse {
    private Long cartItemId;
    private Long userId;
    private String customerName;
    private String emailStatus;
    private String whatsappStatus;
    private String message;
}
