package com.printify.store.service;

import com.printify.store.entity.Product;
import com.printify.store.entity.ProductVariant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingService {

    @Value("${APP_USD_TO_INR_RATE:83.0}")
    private BigDecimal usdToInrRate;

    public ProductVariant resolveVariant(Product product, String variantId) {
        if (product.getVariants() == null || product.getVariants().isEmpty()) {
            return null;
        }

        if (variantId != null && !variantId.isBlank()) {
            return product.getVariants()
                    .stream()
                    .filter(v -> variantId.equals(v.getPrintifyVariantId()))
                    .findFirst()
                    .orElse(null);
        }

        return product.getVariants()
                .stream()
                .filter(ProductVariant::isEnabled)
                .findFirst()
                .orElse(product.getVariants().get(0));
    }

    public BigDecimal getOriginalUnitPrice(Product product, ProductVariant variant) {
        BigDecimal price = variant != null && variant.getPrice() != null
                ? variant.getPrice()
                : product.getPrice();

        if (price == null) {
            throw new IllegalStateException("Product price is missing: " + product.getId());
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }

    public String getOriginalCurrency(Product product, ProductVariant variant) {
        if (variant != null && variant.getCurrency() != null && !variant.getCurrency().isBlank()) {
            return variant.getCurrency().toUpperCase();
        }

        if (product.getCurrency() != null && !product.getCurrency().isBlank()) {
            return product.getCurrency().toUpperCase();
        }

        return "USD";
    }

    public BigDecimal toInr(BigDecimal amount, String currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        String safeCurrency = currency == null ? "USD" : currency.toUpperCase();

        if ("INR".equals(safeCurrency)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }

        if ("USD".equals(safeCurrency)) {
            return amount.multiply(usdToInrRate).setScale(2, RoundingMode.HALF_UP);
        }

        throw new IllegalArgumentException("Unsupported product currency: " + currency);
    }

    public int toPaise(BigDecimal inrAmount) {
        return inrAmount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}