package com.printify.store.repository;

import com.printify.store.entity.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Optional<Order> findByIdAndUserId(String id, String userId);

    Optional<Order> findByPrintifyOrderId(String printifyOrderId);
}