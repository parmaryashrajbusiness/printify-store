package com.printify.store.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Document(collection = "product_reviews")
public class ProductReview extends BaseDocument {
    private String productId;
    private String userId;
    private String userName;
    private Integer rating;
    private String title;
    private String comment;
}