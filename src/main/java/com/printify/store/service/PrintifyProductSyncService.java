package com.printify.store.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.printify.store.entity.Product;
import com.printify.store.entity.ProductVariant;
import com.printify.store.repository.ProductRepository;
import com.printify.store.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class PrintifyProductSyncService {

    private final ProductRepository productRepository;
    private final WebClient webClient;

    @Value("${app.printify.base-url}")
    private String baseUrl;

    @Value("${app.printify.token}")
    private String token;

    @Value("${app.printify.shop-id}")
    private String shopId;

    public int syncProductsFromPrintify() {
        JsonNode response = webClient.get()
                .uri(baseUrl + "/shops/" + shopId + "/products.json")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        JsonNode data = response != null && response.has("data") ? response.get("data") : response;

        if (data == null || !data.isArray()) {
            return 0;
        }

        int synced = 0;

        for (JsonNode item : data) {
            syncOneProduct(item);
            synced++;
        }

        return synced;
    }

    private void syncOneProduct(JsonNode item) {
        String printifyProductId = text(item, "id");
        if (printifyProductId.isBlank()) return;

        Product product = productRepository.findByPrintifyProductId(printifyProductId)
                .orElseGet(Product::new);

        boolean isNew = product.getId() == null;

        String printifyTitle = text(item, "title");
        String printifyDescription = text(item, "description");

        List<String> images = extractImages(item);
        List<ProductVariant> variants = extractVariants(item);

        ProductVariant defaultVariant = variants.stream()
                .filter(ProductVariant::isEnabled)
                .findFirst()
                .orElse(variants.isEmpty() ? null : variants.get(0));

        if (isNew || !product.isContentEditedLocally()) {
            product.setName(printifyTitle);
            product.setSlug(SlugUtil.toSlug(printifyTitle));
            product.setDescription(stripHtml(printifyDescription));
            product.setLongDescription(stripHtml(printifyDescription));

            product.setImageUrls(images);
            product.setImageUrl(images.isEmpty() ? "" : images.get(0));

            product.setBadge(product.getBadge() == null ? "New Drop" : product.getBadge());
            product.setStatus("ACTIVE");

            if (product.getCategorySlug() == null || product.getCategorySlug().isBlank()) {
                product.setCategorySlug("t-shirts");
                product.setCategoryName("T-Shirts");
                product.setSectionSlug("t-shirts");
                product.setSectionName("T-Shirts");
            }
        }

        product.setPrintifyProductId(printifyProductId);
        product.setPrintifyBlueprintId(text(item, "blueprint_id"));
        product.setPrintifyProviderId(text(item, "print_provider_id"));
        product.setPrintifyStatus(text(item, "status"));

        product.setVariants(variants);
        product.setSyncedFromPrintify(true);

        if (defaultVariant != null) {
            product.setDefaultVariantId(defaultVariant.getPrintifyVariantId());
            product.setPrice(defaultVariant.getPrice());
            product.setCompareAtPrice(defaultVariant.getCompareAtPrice());
            product.setCurrency(defaultVariant.getCurrency());
            product.setColorway(defaultVariant.getTitle());
        }

        if (product.getRatingAverage() == null) product.setRatingAverage(0.0);
        if (product.getRatingCount() == null) product.setRatingCount(0);

        productRepository.save(product);
    }

    private List<String> extractImages(JsonNode item) {
        List<String> images = new ArrayList<>();

        JsonNode imagesNode = item.path("images");
        if (imagesNode.isArray()) {
            for (JsonNode image : imagesNode) {
                String src = text(image, "src");
                if (!src.isBlank()) images.add(src);
            }
        }

        return images.stream().distinct().toList();
    }

    private List<ProductVariant> extractVariants(JsonNode item) {
        List<ProductVariant> variants = new ArrayList<>();

        Map<String, Map<String, String>> optionValueNames = extractOptionValueNames(item);

        JsonNode variantsNode = item.path("variants");
        if (!variantsNode.isArray()) return variants;

        for (JsonNode variant : variantsNode) {
            String size = "";
            String color = "";

            JsonNode options = variant.path("options");
            if (options.isArray()) {
                for (JsonNode optionId : options) {
                    Map<String, String> meta = optionValueNames.get(optionId.asText());

                    if (meta == null) continue;

                    String optionName = meta.get("optionName");
                    String value = meta.get("value");

                    if (optionName.contains("size")) {
                        size = value;
                    } else if (optionName.contains("color") || optionName.contains("colour")) {
                        color = value;
                    }
                }
            }

            String title = Stream.of(color, size)
                    .filter(v -> v != null && !v.isBlank())
                    .collect(Collectors.joining(" / "));

            if (title.isBlank()) {
                title = text(variant, "title");
            }

            BigDecimal price = centsToMoney(variant.path("price").asLong(0));
            BigDecimal compareAt = price.multiply(BigDecimal.valueOf(1.35))
                    .setScale(2, RoundingMode.HALF_UP);

            variants.add(ProductVariant.builder()
                    .printifyVariantId(text(variant, "id"))
                    .title(title)
                    .size(size)
                    .color(color)
                    .price(price)
                    .compareAtPrice(compareAt)
                    .currency("USD")
                    .enabled(variant.path("is_enabled").asBoolean(true))
                    .build());
        }

        variants.sort(Comparator.comparingInt(v -> sizeOrder(v.getSize())));

        return variants;
    }

    private int sizeOrder(String size) {
        if (size == null) return 999;

        return switch (size.toUpperCase()) {
            case "XS" -> 0;
            case "S" -> 1;
            case "M" -> 2;
            case "L" -> 3;
            case "XL" -> 4;
            case "2XL" -> 5;
            case "3XL" -> 6;
            case "4XL" -> 7;
            case "5XL" -> 8;
            default -> 999;
        };
    }

    private Map<String, Map<String, String>> extractOptionValueNames(JsonNode item) {
        Map<String, Map<String, String>> map = new HashMap<>();

        JsonNode options = item.path("options");
        if (!options.isArray()) return map;

        for (JsonNode option : options) {
            String optionName = text(option, "name").toLowerCase(Locale.ROOT);

            JsonNode values = option.path("values");
            if (!values.isArray()) continue;

            for (JsonNode value : values) {
                Map<String, String> meta = new HashMap<>();
                meta.put("optionName", optionName);
                meta.put("value", text(value, "title"));

                map.put(text(value, "id"), meta);
            }
        }

        return map;
    }

    private BigDecimal centsToMoney(long cents) {
        return BigDecimal.valueOf(cents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String text(JsonNode node, String key) {
        JsonNode value = node.path(key);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String stripHtml(String value) {
        if (value == null) return "";
        return value.replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}