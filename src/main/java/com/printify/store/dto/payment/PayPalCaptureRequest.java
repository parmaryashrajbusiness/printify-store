package com.printify.store.dto.payment;

import lombok.Data;

@Data
public class PayPalCaptureRequest {
    private String paypalOrderId;
}