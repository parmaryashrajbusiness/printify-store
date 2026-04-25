package com.printify.store.dto.order;

import com.printify.store.entity.OrderItem;
import com.printify.store.entity.ShipmentInfo;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OrderTrackingResponse {
    private String id;
    private String status;
    private String displayStatus;
    private BigDecimal totalAmount;

    private String printifyOrderId;
    private String printifyStatus;
    private String printifyConnectUrl;

    private String trackingUrl;
    private String trackingNumber;
    private String trackingCarrier;
    private LocalDateTime trackingLastSyncedAt;

    private String shippingFullName;
    private String shippingCity;
    private String shippingState;
    private String shippingCountry;

    private List<OrderItem> items;
    private List<ShipmentInfo> shipments;
}