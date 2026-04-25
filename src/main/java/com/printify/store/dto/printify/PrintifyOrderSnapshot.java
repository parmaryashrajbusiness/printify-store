package com.printify.store.dto.printify;

import com.printify.store.entity.ShipmentInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PrintifyOrderSnapshot {
    private String printifyOrderId;
    private String appOrderId;
    private String shopId;
    private String status;
    private String connectUrl;

    private String trackingUrl;
    private String trackingNumber;
    private String trackingCarrier;

    private List<ShipmentInfo> shipments;
}