package com.printify.store.service;

import com.printify.store.dto.cart.AddToCartRequest;
import com.printify.store.dto.cart.UpdateCartItemRequest;
import com.printify.store.entity.CartItem;
import com.printify.store.entity.Product;
import com.printify.store.entity.ProductVariant;
import com.printify.store.entity.User;
import com.printify.store.exception.BadRequestException;
import com.printify.store.exception.ResourceNotFoundException;
import com.printify.store.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductService productService;
    private final PricingService pricingService;

    public List<CartItem> getCartItems(User user) {
        List<CartItem> items = cartItemRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());

        boolean changed = false;

        for (CartItem item : items) {
            CartItem repriced = repriceCartItem(item);
            if (repriced != item) {
                changed = true;
            }
        }

        if (changed) {
            cartItemRepository.saveAll(items);
        }

        return items;
    }

    public void addToCart(User user, AddToCartRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        Product product = productService.getById(request.getProductId());

        ProductVariant variant = pricingService.resolveVariant(product, request.getVariantId());

        String printifyVariantId = variant != null
                ? variant.getPrintifyVariantId()
                : product.getDefaultVariantId();

        if (printifyVariantId == null || printifyVariantId.isBlank()) {
            throw new BadRequestException("Product variant is missing for " + product.getName());
        }

        String cartKeyVariant = printifyVariantId;

        CartItem item = cartItemRepository
                .findByUserIdAndProductIdAndPrintifyVariantId(user.getId(), product.getId(), cartKeyVariant)
                .orElse(
                        CartItem.builder()
                                .userId(user.getId())
                                .productId(product.getId())
                                .productName(product.getName())
                                .productSlug(product.getSlug())
                                .imageUrl(product.getImageUrl())
                                .quantity(0)
                                .printifyVariantId(cartKeyVariant)
                                .build()
                );

        BigDecimal originalPrice = pricingService.getOriginalUnitPrice(product, variant);
        String originalCurrency = pricingService.getOriginalCurrency(product, variant);
        BigDecimal inrPrice = pricingService.toInr(originalPrice, originalCurrency);

        item.setProductName(product.getName());
        item.setProductSlug(product.getSlug());
        item.setImageUrl(product.getImageUrl());
        item.setColorway(
                variant != null
                        ? buildVariantTitle(variant)
                        : product.getColorway()
        );

        item.setOriginalUnitPrice(originalPrice);
        item.setOriginalCurrency(originalCurrency);

        item.setUnitPrice(inrPrice);
        item.setUnitCurrency("INR");

        item.setVariantTitle(
                variant != null
                        ? buildVariantTitle(variant)
                        : product.getColorway()
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

        if (request.getQuantity() == null || request.getQuantity() < 1) {
            throw new BadRequestException("Quantity must be at least 1");
        }

        item.setQuantity(request.getQuantity());
        repriceCartItem(item);
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

    private CartItem repriceCartItem(CartItem item) {
        Product product = productService.getById(item.getProductId());
        ProductVariant variant = pricingService.resolveVariant(product, item.getPrintifyVariantId());

        BigDecimal originalPrice = pricingService.getOriginalUnitPrice(product, variant);
        String originalCurrency = pricingService.getOriginalCurrency(product, variant);
        BigDecimal inrPrice = pricingService.toInr(originalPrice, originalCurrency);

        item.setOriginalUnitPrice(originalPrice);
        item.setOriginalCurrency(originalCurrency);
        item.setUnitPrice(inrPrice);
        item.setUnitCurrency("INR");

        if (variant != null) {
            item.setPrintifyVariantId(variant.getPrintifyVariantId());
            item.setVariantTitle(buildVariantTitle(variant));
            item.setColorway(buildVariantTitle(variant));
        }

        return item;
    }

    private String buildVariantTitle(ProductVariant variant) {
        String color = variant.getColor() == null ? "" : variant.getColor();
        String size = variant.getSize() == null ? "" : variant.getSize();

        String title = (color + " / " + size).replaceAll("^ / | / $", "").trim();

        if (!title.isBlank()) return title;

        return variant.getTitle();
    }
}