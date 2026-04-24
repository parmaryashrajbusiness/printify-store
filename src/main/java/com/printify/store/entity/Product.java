package com.printify.store.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product extends BaseDocument {
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private String badge;
    private String colorway;

    private BigDecimal price;
    private BigDecimal compareAtPrice;

    private Double rating;
    private Integer reviewCount;
    private boolean featured;
    private String status;

    private String sectionId;
    private String sectionSlug;
    private String sectionName;

    private String printifyProductId;
    private String printifyVariantId;
    private String printifyBlueprintId;
    private String printifyProviderId;
}