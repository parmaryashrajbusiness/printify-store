package com.printify.store.controller;

import com.printify.store.entity.User;
import com.printify.store.entity.WishlistItem;
import com.printify.store.service.CurrentUserService;
import com.printify.store.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<WishlistItem> getWishlist(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return wishlistService.getWishlist(user);
    }

    @PostMapping("/{productId}")
    public void toggleWishlist(Authentication authentication, @PathVariable String productId) {
        User user = currentUserService.getCurrentUser(authentication);
        wishlistService.toggleWishlist(user, productId);
    }
}