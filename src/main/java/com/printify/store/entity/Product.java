package com.printify.store.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
    private String longDescription;

    private String imageUrl;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    private String badge;
    private String colorway;

    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String currency;

    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    private String defaultVariantId;

    private Double ratingAverage;
    private Integer ratingCount;

    private boolean featured;
    private String status;

    private String sectionId;
    private String sectionSlug;
    private String sectionName;

    private String categorySlug;
    private String categoryName;

    private String subCategorySlug;
    private String subCategoryName;

    private String material;
    private String fit;
    private String productType;
    private String printType;

    private String printifyProductId;
    private String printifyBlueprintId;
    private String printifyProviderId;

    private boolean syncedFromPrintify;
    private boolean contentEditedLocally;

    private String printifyStatus;
}