package com.printify.store.entity;

import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "checkout_quotes")
public class CheckoutQuote extends BaseDocument {

    private String userId;

    @Indexed(unique = true)
    private String razorpayOrderId;

    private String status; // CREATED, PAID, EXPIRED, FAILED

    private String shippingCountry;
    private String paymentCurrency;

    private BigDecimal subtotal;
    private BigDecimal feeBufferAmount;
    private BigDecimal grandTotal;
    private Integer amountMinorUnit;

    private String checkoutSnapshotJson;
    private List<OrderItem> items;

    private String razorpayPaymentId;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
}