package com.printify.store.entity;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String productId;
    private String productName;
    private String productSlug;
    private String imageUrl;
    private String colorway;
    private Integer quantity;
    private BigDecimal unitPrice;

    private String printifyProductId;
    private String printifyVariantId;
}