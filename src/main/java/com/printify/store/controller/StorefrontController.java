package com.printify.store.controller;

import com.printify.store.entity.Product;
import com.printify.store.entity.ProductSection;
import com.printify.store.service.ProductService;
import com.printify.store.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storefront")
@RequiredArgsConstructor
public class StorefrontController {

    private final SectionService sectionService;
    private final ProductService productService;

    @GetMapping("/home")
    public Map<String, Object> getHome() {
        return Map.of(
                "sections", sectionService.getVisibleSections(),
                "featuredProducts", productService.getFeaturedProducts()
        );
    }

    @GetMapping("/sections")
    public List<ProductSection> getSections() {
        return sectionService.getVisibleSections();
    }

    @GetMapping("/products")
    public List<Product> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort
    ) {
        String selectedCategory = category != null ? category : section;
        return productService.getProducts(selectedCategory, subCategory, search, sort);
    }

    @GetMapping("/products/featured")
    public List<Product> getFeaturedProducts() {
        return productService.getFeaturedProducts();
    }

    @GetMapping("/products/{slug}")
    public Product getProductBySlug(@PathVariable String slug) {
        return productService.getBySlug(slug);
    }

    @GetMapping("/products/{productId}/similar")
    public List<Product> getSimilarProducts(@PathVariable String productId) {
        return productService.getSimilarProducts(productId);
    }
}