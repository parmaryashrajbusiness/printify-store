package com.printify.store.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentInfo {
    private String carrier;
    private String trackingNumber;
    private String trackingUrl;
    private String status;
}