package com.printify.store.controller;

import com.printify.store.dto.cart.AddToCartRequest;
import com.printify.store.dto.cart.UpdateCartItemRequest;
import com.printify.store.entity.CartItem;
import com.printify.store.entity.User;
import com.printify.store.service.CartService;
import com.printify.store.service.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<CartItem> getCart(Authentication authentication) {
        User user = currentUserService.getCurrentUser(authentication);
        return cartService.getCartItems(user);
    }

    @PostMapping
    public void addToCart(Authentication authentication, @Valid @RequestBody AddToCartRequest request) {
        User user = currentUserService.getCurrentUser(authentication);
        cartService.addToCart(user, request);
    }

    @PatchMapping("/{itemId}")
    public void updateCartItem(Authentication authentication,
                               @PathVariable String itemId,
                               @Valid @RequestBody UpdateCartItemRequest request) {
        User user = currentUserService.getCurrentUser(authentication);
        cartService.updateQuantity(user, itemId, request);
    }

    @DeleteMapping("/{itemId}")
    public void removeCartItem(Authentication authentication, @PathVariable String itemId) {
        User user = currentUserService.getCurrentUser(authentication);
        cartService.removeItem(user, itemId);
    }
}