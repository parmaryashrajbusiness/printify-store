package com.printify.store.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order extends BaseDocument {

    private String userId;
    private String status;
    private BigDecimal totalAmount;

    private String printifyOrderId;
    private String printifyAppOrderId;
    private String printifyShopId;
    private String printifyStatus;
    private String printifyConnectUrl;

    private String trackingUrl;
    private String trackingNumber;
    private String trackingCarrier;
    private LocalDateTime trackingLastSyncedAt;

    private String shippingFullName;
    private String shippingEmail;
    private String shippingPhone;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;

    private List<OrderItem> items;
    private List<ShipmentInfo> shipments;

    private String paymentMethod;
    private String paymentStatus;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal paidAmount;
    private String paidCurrency;

    private String paymentProvider;
    private String paymentCurrency;
    private Integer paidAmountMinorUnit;
    private String checkoutQuoteId;

    private String paypalOrderId;
    private String paypalCaptureId;
}