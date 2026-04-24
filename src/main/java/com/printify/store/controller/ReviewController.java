package com.printify.store.controller;

import com.printify.store.dto.review.ReviewRequest;
import com.printify.store.entity.ProductReview;
import com.printify.store.entity.User;
import com.printify.store.repository.UserRepository;
import com.printify.store.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping("/products/{productId}/reviews")
    public List<ProductReview> getReviews(@PathVariable String productId) {
        return reviewService.getReviews(productId);
    }

    @PostMapping("/products/{productId}/reviews")
    public ProductReview createReview(
            @PathVariable String productId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        User user = getLoggedInUser(authentication);
        return reviewService.create(productId, request, user);
    }

    @PutMapping("/reviews/{reviewId}")
    public ProductReview updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication
    ) {
        User user = getLoggedInUser(authentication);
        return reviewService.update(reviewId, request, user);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public void deleteReview(
            @PathVariable String reviewId,
            Authentication authentication
    ) {
        User user = getLoggedInUser(authentication);
        reviewService.delete(reviewId, user);
    }

    private User getLoggedInUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User is not authenticated.");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Logged in user not found."));
    }
}