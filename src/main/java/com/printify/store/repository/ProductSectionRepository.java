package com.printify.store.repository;

import com.printify.store.entity.ProductSection;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductSectionRepository extends MongoRepository<ProductSection, String> {
    Optional<ProductSection> findBySlug(String slug);
    List<ProductSection> findAllByVisibleTrueOrderBySortOrderAsc();
}