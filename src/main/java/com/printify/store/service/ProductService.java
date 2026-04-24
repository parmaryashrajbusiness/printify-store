package com.printify.store.service;

import com.printify.store.dto.admin.ProductRequest;
import com.printify.store.entity.Product;
import com.printify.store.entity.ProductSection;
import com.printify.store.exception.ResourceNotFoundException;
import com.printify.store.repository.ProductRepository;
import com.printify.store.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
                            contains(p.getLongDescription(), q) ||
                            contains(p.getCategoryName(), q) ||
                            contains(p.getSubCategoryName(), q) ||
                            contains(p.getBadge(), q) ||
                            contains(p.getColorway(), q)
            );
        }

        List<Product> products = stream.toList();

        if ("lowToHigh".equalsIgnoreCase(sort)) {
            return products.stream()
                    .sorted(Comparator.comparing(
                            Product::getPrice,
                            Comparator.nullsLast(BigDecimal::compareTo)
                    ))
                    .toList();
        }

        if ("highToLow".equalsIgnoreCase(sort)) {
            return products.stream()
                    .sorted(Comparator.comparing(
                            Product::getPrice,
                            Comparator.nullsLast(BigDecimal::compareTo)
                    ).reversed())
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

    public List<Product> getFeaturedProducts() {
        return productRepository.findAll().stream()
                .filter(p -> p.getStatus() == null || "ACTIVE".equalsIgnoreCase(p.getStatus()))
                .filter(Product::isFeatured)
                .sorted(Comparator.comparing(Product::getRatingAverage, Comparator.nullsLast(Double::compareTo)).reversed())
                .limit(8)
                .toList();
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

        List<String> imageUrls = request.getImageUrls() == null
                ? List.of()
                : request.getImageUrls();

        String mainImage = request.getImageUrl();
        if ((mainImage == null || mainImage.isBlank()) && !imageUrls.isEmpty()) {
            mainImage = imageUrls.get(0);
        }

        Product product = Product.builder()
                .name(request.getName())
                .slug(SlugUtil.toSlug(request.getName()))
                .description(request.getDescription())
                .longDescription(request.getLongDescription())

                .imageUrl(mainImage)
                .imageUrls(imageUrls)

                .badge(request.getBadge())
                .colorway(request.getColorway())

                .price(request.getPrice())
                .compareAtPrice(request.getCompareAtPrice())
                .currency(request.getCurrency())

                .featured(request.isFeatured())
                .status(request.getStatus())

                .sectionId(section.getId())
                .sectionSlug(section.getSlug())
                .sectionName(section.getName())

                .categorySlug(request.getCategorySlug())
                .categoryName(request.getCategoryName())
                .subCategorySlug(request.getSubCategorySlug())
                .subCategoryName(request.getSubCategoryName())

                .material(request.getMaterial())
                .fit(request.getFit())
                .productType(request.getProductType())
                .printType(request.getPrintType())

                .printifyProductId(request.getPrintifyProductId())
                .printifyBlueprintId(request.getPrintifyBlueprintId())
                .printifyProviderId(request.getPrintifyProviderId())

                .variants(request.getVariants() == null ? List.of() : request.getVariants())
                .defaultVariantId(request.getDefaultVariantId())

                .ratingAverage(0.0)
                .ratingCount(0)

                .syncedFromPrintify(false)
                .contentEditedLocally(true)
                .build();

        return productRepository.save(product);
    }

    public Product update(String id, ProductRequest request) {
        Product product = getById(id);
        ProductSection section = sectionService.getById(request.getSectionId());

        List<String> imageUrls = request.getImageUrls() == null
                ? List.of()
                : request.getImageUrls();

        String mainImage = request.getImageUrl();
        if ((mainImage == null || mainImage.isBlank()) && !imageUrls.isEmpty()) {
            mainImage = imageUrls.get(0);
        }

        product.setName(request.getName());
        product.setSlug(SlugUtil.toSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setLongDescription(request.getLongDescription());

        product.setImageUrl(mainImage);
        product.setImageUrls(imageUrls);

        product.setBadge(request.getBadge());
        product.setColorway(request.getColorway());

        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setCurrency(request.getCurrency());

        product.setFeatured(request.isFeatured());
        product.setStatus(request.getStatus());

        product.setSectionId(section.getId());
        product.setSectionSlug(section.getSlug());
        product.setSectionName(section.getName());

        product.setCategorySlug(request.getCategorySlug());
        product.setCategoryName(request.getCategoryName());
        product.setSubCategorySlug(request.getSubCategorySlug());
        product.setSubCategoryName(request.getSubCategoryName());

        product.setMaterial(request.getMaterial());
        product.setFit(request.getFit());
        product.setProductType(request.getProductType());
        product.setPrintType(request.getPrintType());

        product.setPrintifyProductId(request.getPrintifyProductId());
        product.setPrintifyBlueprintId(request.getPrintifyBlueprintId());
        product.setPrintifyProviderId(request.getPrintifyProviderId());

        if (request.getVariants() != null) {
            product.setVariants(request.getVariants());
        }

        if (request.getDefaultVariantId() != null && !request.getDefaultVariantId().isBlank()) {
            product.setDefaultVariantId(request.getDefaultVariantId());
        }

        product.setContentEditedLocally(true);

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

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private boolean same(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }
}