package com.printify.store.service;

import com.printify.store.dto.review.ReviewRequest;
import com.printify.store.entity.Product;
import com.printify.store.entity.ProductReview;
import com.printify.store.entity.User;
import com.printify.store.exception.BadRequestException;
import com.printify.store.exception.ResourceNotFoundException;
import com.printify.store.repository.ProductRepository;
import com.printify.store.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    public List<ProductReview> getReviews(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public ProductReview create(String productId, ReviewRequest request, User user) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        reviewRepository.findByProductIdAndUserId(productId, user.getId())
                .ifPresent(r -> {
                    throw new BadRequestException("You already reviewed this product.");
                });

        ProductReview review = ProductReview.builder()
                .productId(productId)
                .userId(user.getId())
                .userName(user.getFullName())
                .rating(request.getRating())
                .title(clean(request.getTitle()))
                .comment(clean(request.getComment()))
                .build();

        ProductReview saved = reviewRepository.save(review);
        refreshProductRating(product);
        return saved;
    }

    public ProductReview update(String reviewId, ReviewRequest request, User user) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUserId().equals(user.getId())) {
            throw new BadRequestException("You can update only your own review.");
        }

        review.setRating(request.getRating());
        review.setTitle(clean(request.getTitle()));
        review.setComment(clean(request.getComment()));

        ProductReview saved = reviewRepository.save(review);

        Product product = productRepository.findById(review.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        refreshProductRating(product);
        return saved;
    }

    public void delete(String reviewId, User user) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUserId().equals(user.getId())) {
            throw new BadRequestException("You can delete only your own review.");
        }

        String productId = review.getProductId();
        reviewRepository.delete(review);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        refreshProductRating(product);
    }

    private void refreshProductRating(Product product) {
        List<ProductReview> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());

        int count = reviews.size();
        double average = count == 0 ? 0.0 :
                reviews.stream().mapToInt(ProductReview::getRating).average().orElse(0.0);

        product.setRatingAverage(Math.round(average * 10.0) / 10.0);
        product.setRatingCount(count);

        productRepository.save(product);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}