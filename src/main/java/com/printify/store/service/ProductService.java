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

    public List<Product> getProducts(String category, String subCategory, String search, String sort) {
        Stream<Product> stream = productRepository.findAll().stream()
                .filter(p -> p.getStatus() == null || "ACTIVE".equalsIgnoreCase(p.getStatus()));

        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            stream = stream.filter(p ->
                    category.equalsIgnoreCase(p.getSectionSlug()) ||
                            category.equalsIgnoreCase(p.getCategorySlug())
            );
        }

        if (subCategory != null && !subCategory.isBlank() && !"all".equalsIgnoreCase(subCategory)) {
            stream = stream.filter(p -> subCategory.equalsIgnoreCase(p.getSubCategorySlug()));
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase(Locale.ROOT);

            stream = stream.filter(p ->
                    contains(p.getName(), q) ||
                            contains(p.getDescription(), q) ||
                            contains(p.getCategoryName(), q) ||
                            contains(p.getSubCategoryName(), q)
            );
        }

        List<Product> products = stream.toList();

        if ("lowToHigh".equalsIgnoreCase(sort)) {
            return products.stream()
                    .sorted(Comparator.comparing(Product::getPrice))
                    .toList();
        }

        if ("highToLow".equalsIgnoreCase(sort)) {
            return products.stream()
                    .sorted(Comparator.comparing(Product::getPrice).reversed())
                    .toList();
        }

        if ("rating".equalsIgnoreCase(sort)) {
            return products.stream()
                    .sorted(Comparator.comparing(
                            Product::getRatingAverage,
                            Comparator.nullsLast(Double::compareTo)
                    ).reversed())
                    .toList();
        }

        return products.stream()
                .sorted(Comparator.comparing(Product::isFeatured).reversed())
                .toList();
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
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
                .featured(request.isFeatured())
                .status(request.getStatus())
                .sectionId(section.getId())
                .sectionSlug(section.getSlug())
                .sectionName(section.getName())
                .printifyProductId(request.getPrintifyProductId())
                .printifyVariantId(request.getPrintifyVariantId())
                .printifyBlueprintId(request.getPrintifyBlueprintId())
                .printifyProviderId(request.getPrintifyProviderId())
                .longDescription(request.getLongDescription())
                .images(request.getImages() == null ? List.of() : request.getImages())
                .ratingAverage(0.0)
                .ratingCount(0)
                .categorySlug(request.getCategorySlug())
                .categoryName(request.getCategoryName())
                .subCategorySlug(request.getSubCategorySlug())
                .subCategoryName(request.getSubCategoryName())
                .material(request.getMaterial())
                .fit(request.getFit())
                .productType(request.getProductType())
                .printType(request.getPrintType())
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
        product.setFeatured(request.isFeatured());
        product.setStatus(request.getStatus());
        product.setSectionId(section.getId());
        product.setSectionSlug(section.getSlug());
        product.setSectionName(section.getName());
        product.setPrintifyProductId(request.getPrintifyProductId());
        product.setPrintifyVariantId(request.getPrintifyVariantId());
        product.setPrintifyBlueprintId(request.getPrintifyBlueprintId());
        product.setPrintifyProviderId(request.getPrintifyProviderId());
        product.setLongDescription(request.getLongDescription());
        product.setImages(request.getImages() == null ? List.of() : request.getImages());

        product.setCategorySlug(request.getCategorySlug());
        product.setCategoryName(request.getCategoryName());

        product.setSubCategorySlug(request.getSubCategorySlug());
        product.setSubCategoryName(request.getSubCategoryName());

        product.setMaterial(request.getMaterial());
        product.setFit(request.getFit());
        product.setProductType(request.getProductType());
        product.setPrintType(request.getPrintType());

        return productRepository.save(product);
    }

    public void delete(String id) {
        productRepository.deleteById(id);
    }

    public List<Product> getSimilarProducts(String productId) {
        Product product = getById(productId);

        return productRepository.findAll().stream()
                .filter(p -> !p.getId().equals(product.getId()))
                .filter(p -> p.getStatus() == null || "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .filter(p ->
                        same(p.getSubCategorySlug(), product.getSubCategorySlug()) ||
                                same(p.getCategorySlug(), product.getCategorySlug()) ||
                                same(p.getSectionSlug(), product.getSectionSlug())
                )
                .sorted(Comparator.comparing(Product::isFeatured).reversed())
                .limit(8)
                .toList();
    }

    private boolean same(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }
}