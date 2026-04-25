package com.printify.store.service;

import com.printify.store.entity.Product;
import com.printify.store.entity.ProductVariant;
import com.printify.store.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;

@Service
public class PricingService {

    private static final Set<String> ALLOWED_COUNTRIES = Set.of("IN", "US", "AU", "DE", "FR");

    @Value("${APP_USD_TO_INR_RATE:83.0}")
    private BigDecimal usdToInrRate;

    @Value("${APP_USD_TO_AUD_RATE:1.55}")
    private BigDecimal usdToAudRate;

    @Value("${APP_USD_TO_EUR_RATE:0.92}")
    private BigDecimal usdToEurRate;

    @Value("${APP_INTERNATIONAL_FEE_BUFFER_PERCENT:7.0}")
    private BigDecimal internationalFeeBufferPercent;

    public String normalizeCountry(String country) {
        if (country == null || country.isBlank()) {
            throw new BadRequestException("Shipping country is required");
        }

        String normalized = country.trim().toUpperCase();

        Map<String, String> aliases = Map.of(
                "INDIA", "IN",
                "UNITED STATES", "US",
                "USA", "US",
                "AUSTRALIA", "AU",
                "GERMANY", "DE",
                "FRANCE", "FR"
        );

        normalized = aliases.getOrDefault(normalized, normalized);

        if (!ALLOWED_COUNTRIES.contains(normalized)) {
            throw new BadRequestException("We currently do not ship to this country");
        }

        return normalized;
    }

    public String currencyForCountry(String country) {
        return switch (normalizeCountry(country)) {
            case "IN" -> "INR";
            case "US" -> "USD";
            case "AU" -> "AUD";
            case "DE", "FR" -> "EUR";
            default -> throw new BadRequestException("Unsupported country");
        };
    }

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
            throw new BadRequestException("Product price is missing");
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

    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        String from = fromCurrency == null ? "USD" : fromCurrency.toUpperCase();
        String to = toCurrency == null ? "USD" : toCurrency.toUpperCase();

        if (from.equals(to)) {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }

        if ("USD".equals(from)) {
            return switch (to) {
                case "INR" -> amount.multiply(usdToInrRate).setScale(2, RoundingMode.HALF_UP);
                case "AUD" -> amount.multiply(usdToAudRate).setScale(2, RoundingMode.HALF_UP);
                case "EUR" -> amount.multiply(usdToEurRate).setScale(2, RoundingMode.HALF_UP);
                default -> throw new BadRequestException("Unsupported currency: " + to);
            };
        }

        throw new BadRequestException("Unsupported source currency: " + from);
    }

    public BigDecimal addFeeBuffer(BigDecimal subtotal, String country) {
        String normalized = normalizeCountry(country);

        if ("IN".equals(normalized)) {
            return subtotal.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal multiplier = BigDecimal.ONE.add(
                internationalFeeBufferPercent.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
        );

        return subtotal.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public int toMinorUnit(BigDecimal amount, String currency) {
        if (!Set.of("INR", "USD", "AUD", "EUR").contains(currency.toUpperCase())) {
            throw new BadRequestException("Unsupported payment currency");
        }

        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}