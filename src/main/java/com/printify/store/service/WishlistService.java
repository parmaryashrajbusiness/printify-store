package com.printify.store.service;

import com.printify.store.entity.User;
import com.printify.store.entity.WishlistItem;
import com.printify.store.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;

    public List<WishlistItem> getWishlist(User user) {
        return wishlistItemRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public void toggleWishlist(User user, String productId) {
        wishlistItemRepository.findByUserIdAndProductId(user.getId(), productId)
                .ifPresentOrElse(
                        wishlistItemRepository::delete,
                        () -> wishlistItemRepository.save(
                                WishlistItem.builder()
                                        .userId(user.getId())
                                        .productId(productId)
                                        .build()
                        )
                );
    }
}