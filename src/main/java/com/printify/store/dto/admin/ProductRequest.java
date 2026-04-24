package com.printify.store.dto.admin;

import com.printify.store.entity.ProductVariant;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductRequest {

    private String name;
    private String description;
    private String longDescription;

    private String imageUrl;
    private List<String> imageUrls;

    private String badge;
    private String colorway;

    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private String currency;

    private boolean featured;
    private String status;

    private String sectionId;

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

    private List<ProductVariant> variants;
    private String defaultVariantId;
}