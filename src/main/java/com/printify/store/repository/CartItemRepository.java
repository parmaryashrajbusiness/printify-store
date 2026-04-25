package com.printify.store.repository;

import com.printify.store.entity.CartItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends MongoRepository<CartItem, String> {
    List<CartItem> findAllByUserIdOrderByCreatedAtDesc(String userId);
    Optional<CartItem> findByUserIdAndProductId(String userId, String productId);
    void deleteAllByUserId(String userId);
    Optional<CartItem> findByUserIdAndProductIdAndPrintifyVariantId(
            String userId,
            String productId,
            String printifyVariantId
    );
}