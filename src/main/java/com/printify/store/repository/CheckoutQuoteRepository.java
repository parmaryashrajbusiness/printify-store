package com.printify.store.repository;

import com.printify.store.entity.CheckoutQuote;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CheckoutQuoteRepository extends MongoRepository<CheckoutQuote, String> {
    Optional<CheckoutQuote> findByRazorpayOrderId(String razorpayOrderId);
}