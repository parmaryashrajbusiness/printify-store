package com.printify.store.repository;

import com.printify.store.entity.ProductReview;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends MongoRepository<ProductReview, String> {
    List<ProductReview> findByProductIdOrderByCreatedAtDesc(String productId);
    Optional<ProductReview> findByProductIdAndUserId(String productId, String userId);
    long countByProductId(String productId);
}