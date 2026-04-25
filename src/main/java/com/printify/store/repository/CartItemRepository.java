package com.printify.store.repository;

import com.printify.store.entity.CartItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends MongoRepository<CartItem, String> {

    List<CartItem> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Optional<CartItem> findByUserIdAndProductId(String userId, String productId);

    Optional<CartItem> findByUserIdAndProductIdAndPrintifyVariantId(
            String userId,
            String productId,
            String printifyVariantId
    );

    void deleteAllByUserId(String userId);

    List<CartItem> findByUserId(String userId);

}