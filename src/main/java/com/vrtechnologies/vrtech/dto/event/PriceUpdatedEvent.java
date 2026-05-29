package com.vrtechnologies.vrtech.dto.event;

import org.springframework.context.ApplicationEvent;
import java.math.BigDecimal;

public class PriceUpdatedEvent extends ApplicationEvent {

    private final Long productId;
    private final BigDecimal oldPrice;
    private final BigDecimal newPrice;

    public PriceUpdatedEvent(Object source, Long productId, BigDecimal oldPrice, BigDecimal newPrice) {
        super(source);
        this.productId = productId;
        this.oldPrice = oldPrice;
        this.newPrice = newPrice;
    }

    public Long getProductId() {
        return productId;
    }

    public BigDecimal getOldPrice() {
        return oldPrice;
    }

    public BigDecimal getNewPrice() {
        return newPrice;
    }
}
