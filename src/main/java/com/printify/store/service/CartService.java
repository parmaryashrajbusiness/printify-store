package com.printify.store.service;

import com.printify.store.dto.cart.AddToCartRequest;
import com.printify.store.dto.cart.UpdateCartItemRequest;
import com.printify.store.entity.CartItem;
import com.printify.store.entity.Product;
import com.printify.store.entity.User;
import com.printify.store.exception.ResourceNotFoundException;
import com.printify.store.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductService productService;

    public List<CartItem> getCartItems(User user) {
        return cartItemRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }

    public void addToCart(User user, AddToCartRequest request) {
        Product product = productService.getById(request.getProductId());

        CartItem item = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId())
                .orElse(
                        CartItem.builder()
                                .userId(user.getId())
                                .productId(product.getId())
                                .productName(product.getName())
                                .productSlug(product.getSlug())
                                .imageUrl(product.getImageUrl())
                                .colorway(product.getColorway())
                                .unitPrice(product.getPrice())
                                .quantity(0)
                                .build()
                );

        item.setQuantity(item.getQuantity() + request.getQuantity());
        cartItemRepository.save(item);
    }

    public void updateQuantity(User user, String itemId, UpdateCartItemRequest request) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("Cart item not found");
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);
    }

    public void removeItem(User user, String itemId) {
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getUserId().equals(user.getId())) {
            throw new ResourceNotFoundException("Cart item not found");
        }

        cartItemRepository.delete(item);
    }

    public void clearCart(User user) {
        cartItemRepository.deleteAllByUserId(user.getId());
    }
}