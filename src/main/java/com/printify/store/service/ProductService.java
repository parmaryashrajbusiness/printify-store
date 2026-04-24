package com.printify.store.service;

import com.printify.store.dto.admin.ProductRequest;
import com.printify.store.entity.Product;
import com.printify.store.entity.ProductSection;
import com.printify.store.exception.ResourceNotFoundException;
import com.printify.store.repository.ProductRepository;
import com.printify.store.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final SectionService sectionService;

    public List<Product> getProducts(String section, String search, String sort) {
        Stream<Product> stream = productRepository.findAll().stream();

        if (section != null && !section.isBlank()) {
            stream = stream.filter(p -> section.equalsIgnoreCase(p.getSectionSlug()));
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase(Locale.ROOT);
            stream = stream.filter(p ->
                    (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(q)) ||
                            (p.getDescription() != null && p.getDescription().toLowerCase(Locale.ROOT).contains(q))
            );
        }

        List<Product> products = stream.toList();

        if ("lowToHigh".equalsIgnoreCase(sort)) {
            products = products.stream().sorted(Comparator.comparing(Product::getPrice)).toList();
        } else if ("highToLow".equalsIgnoreCase(sort)) {
            products = products.stream().sorted(Comparator.comparing(Product::getPrice).reversed()).toList();
        } else if ("rating".equalsIgnoreCase(sort)) {
            products = products.stream().sorted(Comparator.comparing(Product::getRating, Comparator.nullsLast(Double::compareTo)).reversed()).toList();
        } else {
            products = products.stream().sorted(Comparator.comparing(Product::isFeatured).reversed()).toList();
        }

        return products;
    }

    public List<Product> getFeaturedProducts() {
        return productRepository.findAllByFeaturedTrue();
    }

    public Product getBySlug(String slug) {
        return productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    public Product getById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public Product create(ProductRequest request) {
        ProductSection section = sectionService.getById(request.getSectionId());

        Product product = Product.builder()
                .name(request.getName())
                .slug(SlugUtil.toSlug(request.getName()))
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .badge(request.getBadge())
                .colorway(request.getColorway())
                .price(request.getPrice())
                .compareAtPrice(request.getCompareAtPrice())
                .rating(request.getRating())
                .reviewCount(request.getReviewCount())
                .featured(request.isFeatured())
                .status(request.getStatus())
                .sectionId(section.getId())
                .sectionSlug(section.getSlug())
                .sectionName(section.getName())
                .printifyProductId(request.getPrintifyProductId())
                .printifyVariantId(request.getPrintifyVariantId())
                .printifyBlueprintId(request.getPrintifyBlueprintId())
                .printifyProviderId(request.getPrintifyProviderId())
                .build();

        return productRepository.save(product);
    }

    public Product update(String id, ProductRequest request) {
        Product product = getById(id);
        ProductSection section = sectionService.getById(request.getSectionId());

        product.setName(request.getName());
        product.setSlug(SlugUtil.toSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setBadge(request.getBadge());
        product.setColorway(request.getColorway());
        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setRating(request.getRating());
        product.setReviewCount(request.getReviewCount());
        product.setFeatured(request.isFeatured());
        product.setStatus(request.getStatus());
        product.setSectionId(section.getId());
        product.setSectionSlug(section.getSlug());
        product.setSectionName(section.getName());
        product.setPrintifyProductId(request.getPrintifyProductId());
        product.setPrintifyVariantId(request.getPrintifyVariantId());
        product.setPrintifyBlueprintId(request.getPrintifyBlueprintId());
        product.setPrintifyProviderId(request.getPrintifyProviderId());

        return productRepository.save(product);
    }

    public void delete(String id) {
        productRepository.deleteById(id);
    }
}