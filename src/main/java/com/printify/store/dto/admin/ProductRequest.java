package com.printify.store.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String description;

    private String imageUrl;
    private String badge;
    private String colorway;

    @NotNull
    private BigDecimal price;

    private BigDecimal compareAtPrice;
    private Double rating;
    private Integer reviewCount;
    private boolean featured;
    private String status;

    @NotBlank
    private String sectionId;

    private String printifyProductId;
    private String printifyVariantId;
    private String printifyBlueprintId;
    private String printifyProviderId;

    private String longDescription;
    private List<String> images;

    private String categorySlug;
    private String categoryName;

    private String subCategorySlug;
    private String subCategoryName;

    private String material;
    private String fit;
    private String productType;
    private String printType;
}