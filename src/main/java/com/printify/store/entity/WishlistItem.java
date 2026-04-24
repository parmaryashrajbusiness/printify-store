package com.printify.store.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wishlist_items")
public class WishlistItem extends BaseDocument {
    private String userId;
    private String productId;
}