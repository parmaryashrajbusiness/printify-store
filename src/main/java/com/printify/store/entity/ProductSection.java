package com.printify.store.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_sections")
public class ProductSection extends BaseDocument {
    private String name;
    private String slug;
    private String subtitle;
    private String highlight;
    private boolean visible;
    private Integer sortOrder;
}