package com.eazicut.api.products.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.categories.entity.Category;
import com.eazicut.api.categories.repository.CategoryRepository;
import com.eazicut.api.collections.entity.Collection;
import com.eazicut.api.collections.repository.CollectionRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.products.dto.ProductFilterCriteria;
import com.eazicut.api.products.dto.ProductRequest;
import com.eazicut.api.products.dto.ProductResponse;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductImage;
import com.eazicut.api.products.entity.ProductStatus;
import com.eazicut.api.products.exception.DuplicateSkuException;
import com.eazicut.api.products.exception.DuplicateSlugException;
import com.eazicut.api.products.mapper.ProductMapper;
import com.eazicut.api.products.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Business logic for the Product feature.
 *
 * <p>Every mutating method runs in a read-write transaction (class-level
 * {@link Transactional}); reads override to {@code readOnly = true} so
 * Hibernate can skip dirty-checking.
 *
 * <p><strong>Uniqueness.</strong> Slug and SKU are guarded twice: cheaply
 * via {@code exists…} probes before saving (turns most conflicts into a
 * clean 409 without a failed insert), and by DB unique constraints as
 * the final backstop under race conditions.
 *
 * <p><strong>Category / Collection resolution.</strong> The request carries
 * only UUIDs; the service resolves them to entity references, raising
 * {@link ResourceNotFoundException} (→ 404) when the target doesn't exist.
 * Both are optional — {@code null} unlinks the product.
 *
 * <p><strong>Image collection.</strong> On update the collection is
 * replaced wholesale (PUT semantics). {@code orphanRemoval = true} on the
 * {@code @OneToMany} in {@link Product} handles the deletes.
 *
 * <p><strong>Read paths.</strong> {@link #getById(UUID)} and
 * {@link #getBySlug(String)} use repository queries with {@code @EntityGraph}
 * so images + category + collection load in one SQL — no N+1 on the PDP.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProductService {

    private static final String BRAND_DEFAULT = "Eazi Cut";
    private static final String CURRENCY_DEFAULT = "NGN";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;
    private final ProductMapper mapper;

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        Product product = productRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return mapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", slug));
        return mapper.toResponse(product);
    }

    /**
     * Paginated listing.
     *
     * <p>Stage 3 delegates to {@code findAll(pageable)}; Stage 5 replaces
     * this call with a spec-driven query using the {@code criteria}
     * argument to compose predicates. The public signature does not
     * change between stages.
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> list(ProductFilterCriteria criteria, Pageable pageable) {
        return productRepository.findAll(pageable).map(mapper::toResponse);
    }

    // ---------------------------------------------------------------------
    // Writes
    // ---------------------------------------------------------------------

    public ProductResponse create(ProductRequest request) {
        assertSlugAvailable(request.slug());
        assertSkuAvailable(request.sku());

        Product product = mapper.toEntity(request);
        applyDefaults(product, request);
        product.setCategory(resolveCategory(request.categoryId()));
        product.setCollection(resolveCollection(request.collectionId()));
        replaceImages(product, request);

        Product saved = productRepository.save(product);
        return mapper.toResponse(saved);
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        // Uniqueness guard — only if the caller is changing the identifier.
        if (!product.getSlug().equals(request.slug())) {
            assertSlugAvailable(request.slug());
        }
        if (!product.getSku().equals(request.sku())) {
            assertSkuAvailable(request.sku());
        }

        mapper.updateEntity(request, product);
        applyDefaults(product, request);
        product.setCategory(resolveCategory(request.categoryId()));
        product.setCollection(resolveCollection(request.collectionId()));
        replaceImages(product, request);

        return mapper.toResponse(product);
    }

    /**
     * Soft delete. The {@code @SQLDelete} annotation on {@link Product}
     * rewrites this to an UPDATE of {@code deleted_at}; subsequent reads
     * via Hibernate exclude the row automatically.
     */
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productRepository.deleteById(id);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void assertSlugAvailable(String slug) {
        if (productRepository.existsBySlug(slug)) {
            throw new DuplicateSlugException(slug);
        }
    }

    private void assertSkuAvailable(String sku) {
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }
    }

    private Category resolveCategory(UUID categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    private Collection resolveCollection(UUID collectionId) {
        if (collectionId == null) return null;
        return collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection", collectionId));
    }

    /**
     * Apply server-side defaults for nullable request fields. Kept out of
     * the mapper so the fallback rules are explicit and unit-testable.
     */
    private void applyDefaults(Product product, ProductRequest request) {
        if (isBlank(request.brand())) product.setBrand(BRAND_DEFAULT);
        if (isBlank(request.currency())) product.setCurrency(CURRENCY_DEFAULT);
        if (request.status() == null) product.setStatus(ProductStatus.DRAFT);
        if (request.stockQuantity() == null) product.setStockQuantity(0);
        if (request.featured() == null) product.setFeatured(false);
        if (request.newArrival() == null) product.setNewArrival(false);
        if (request.bestseller() == null) product.setBestseller(false);
    }

    /**
     * Full-replacement image handling for PUT semantics. Existing rows
     * are dropped by orphanRemoval on the OneToMany; new rows carry the
     * bidirectional back-ref via Product.addImage.
     */
    private void replaceImages(Product product, ProductRequest request) {
        product.getImages().clear();
        if (request.images() == null || request.images().isEmpty()) {
            return;
        }
        List<ProductImage> materialised = request.images().stream()
                .map(mapper::toImageEntity)
                .toList();
        materialised.forEach(product::addImage);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
