package com.vrtechnologies.vrtech.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShipmentUpdateRequest {

    @Size(max = 80)
    private String courierName;

    @Size(max = 80)
    private String trackingNumber;

    @Size(max = 500)
    private String trackingUrl;

    /**
     * If true, the server bumps order.status to SHIPPED (when not already shipped/delivered/cancelled)
     * and stamps shippedAt to now if not already set.
     */
    private Boolean markShipped;

    /**
     * If true, clears all shipment fields and resets shippedAt. Other fields are ignored.
     */
    private Boolean clear;
}
