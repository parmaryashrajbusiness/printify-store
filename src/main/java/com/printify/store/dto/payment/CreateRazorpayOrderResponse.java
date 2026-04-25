package com.printify.store.dto.payment;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CreateRazorpayOrderResponse {
    private String razorpayOrderId;
    private String currency;
    private Integer amount;
    private BigDecimal displayAmount;
    private String keyId;
}