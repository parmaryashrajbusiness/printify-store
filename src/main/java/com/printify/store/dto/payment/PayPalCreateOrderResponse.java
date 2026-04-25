package com.printify.store.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayPalCreateOrderResponse {
    private String paypalOrderId;
    private String approvalUrl;
}