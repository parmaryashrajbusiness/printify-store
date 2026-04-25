package com.printify.store.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "cart_items")
public class CartItem extends BaseDocument {

    private String userId;

    private String productId;
    private String productName;
    private String productSlug;
    private String imageUrl;
    private String colorway;

    // Final customer-facing selling price in INR.
    // This is what Razorpay must charge.
    private BigDecimal unitPrice;
    private String unitCurrency;

    // Original product/variant price before conversion.
    private BigDecimal originalUnitPrice;
    private String originalCurrency;

    private Integer quantity;

    private String printifyVariantId;
    private String variantTitle;
}