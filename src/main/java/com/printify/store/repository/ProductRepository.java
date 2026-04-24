package com.printify.store.repository;

import com.printify.store.entity.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
    Optional<Product> findBySlug(String slug);
    List<Product> findAllByFeaturedTrue();
    List<Product> findAllBySectionSlug(String sectionSlug);
}