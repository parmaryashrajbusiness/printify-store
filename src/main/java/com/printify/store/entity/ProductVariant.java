package com.printify.store.entity;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    private String printifyVariantId;

    private String title;
    private String size;
    private String color;

    private BigDecimal price;
    private BigDecimal compareAtPrice;

    private String currency;

    private boolean enabled;
}