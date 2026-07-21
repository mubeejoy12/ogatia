package com.eazicut.api.products.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.eazicut.api.categories.entity.Category;
import com.eazicut.api.categories.repository.CategoryRepository;
import com.eazicut.api.products.dto.ProductFilterCriteria;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;
import com.eazicut.api.products.specification.ProductSpecifications;

/**
 * Repository-slice tests for {@link ProductRepository} and the specification
 * composition it uses at runtime.
 *
 * <p>{@link DataJpaTest} boots only the JPA layer against H2. The main
 * app's {@code @EnableJpaAuditing} is inherited (via the walked-up
 * {@code @SpringBootConfiguration}) so {@code createdAt}/{@code updatedAt}
 * populate correctly without needing a nested test config.
 *
 * <p>The slice's default {@code ddl-auto=create-drop} is overridden to
 * {@code validate} so Flyway (auto-run by the main config) is the schema
 * authority in tests too — the same migrations that build prod's schema
 * build the test schema. Any drift between the entity model and the
 * migration surfaces here immediately, not in a prod deploy.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class ProductRepositoryTest {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;

    private Product onyx;
    private Product ivory;

    @BeforeEach
    void seed() {
        Category suits = category("Suits", "suits");
        Category shirts = category("Shirts", "shirts");

        onyx = productRepository.save(product("The Onyx Two-Piece", "onyx-two-piece-suit",
                "EC-ONYX-2P", new BigDecimal("650000"), 5,
                ProductStatus.ACTIVE, true, false, false, suits));
        ivory = productRepository.save(product("Ivory Three-Piece", "ivory-three-piece",
                "EC-IVORY-3P", new BigDecimal("950000"), 3,
                ProductStatus.ACTIVE, false, true, false, suits));
        productRepository.save(product("Poplin Shirt", "poplin-shirt",
                "EC-POPLIN-01", new BigDecimal("95000"), 50,
                ProductStatus.ACTIVE, false, false, true, shirts));
    }

    // ---------------------------------------------------------------------
    // findBySlug / findBySku / existsBy…
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("findBySlug — returns the matching product")
    void findBySlugReturnsProduct() {
        Optional<Product> found = productRepository.findBySlug("onyx-two-piece-suit");
        assertThat(found).isPresent();
        assertThat(found.get().getSku()).isEqualTo("EC-ONYX-2P");
    }

    @Test
    @DisplayName("findBySlug — after soft delete returns empty (SQLRestriction filters)")
    void findBySlugAfterSoftDeleteReturnsEmpty() {
        productRepository.deleteById(onyx.getId());
        productRepository.flush();
        assertThat(productRepository.findBySlug("onyx-two-piece-suit")).isEmpty();
    }

    @Test
    @DisplayName("existsBySlug / existsBySku behave symmetrically")
    void existsProbes() {
        assertThat(productRepository.existsBySlug("ivory-three-piece")).isTrue();
        assertThat(productRepository.existsBySlug("does-not-exist")).isFalse();
        assertThat(productRepository.existsBySku("EC-POPLIN-01")).isTrue();
        assertThat(productRepository.existsBySku("EC-DOES-NOT-EXIST")).isFalse();
    }

    // ---------------------------------------------------------------------
    // Specifications
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("Specifications: matching(empty) returns every non-deleted product")
    void emptyMatchesAll() {
        List<Product> results = productRepository.findAll(
                ProductSpecifications.matching(ProductFilterCriteria.empty()));
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("Specifications: featured=true returns only featured products")
    void featuredFilter() {
        List<Product> results = productRepository.findAll(
                ProductSpecifications.matching(
                        ProductFilterCriteria.empty().withFeatured(true)));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSlug()).isEqualTo("onyx-two-piece-suit");
    }

    @Test
    @DisplayName("Specifications: minPrice ≥ 500000 excludes the low-priced poplin")
    void priceLowerBound() {
        List<Product> results = productRepository.findAll(
                ProductSpecifications.priceAtLeast(new BigDecimal("500000")));
        assertThat(results).extracting(Product::getSlug)
                .containsExactlyInAnyOrder("onyx-two-piece-suit", "ivory-three-piece");
    }

    @Test
    @DisplayName("Specifications: available=true excludes non-ACTIVE and zero-stock products")
    void availableFilter() {
        Product mutable = productRepository.findById(ivory.getId()).orElseThrow();
        mutable.setStatus(ProductStatus.OUT_OF_STOCK);
        productRepository.saveAndFlush(mutable);

        List<Product> available = productRepository.findAll(
                ProductSpecifications.isAvailable(true));

        assertThat(available).extracting(Product::getSlug)
                .containsExactlyInAnyOrder("onyx-two-piece-suit", "poplin-shirt");
    }

    @Test
    @DisplayName("Specifications: keyword search is case-insensitive across name")
    void keywordCaseInsensitive() {
        List<Product> results = productRepository.findAll(
                ProductSpecifications.hasKeyword("POPLIN"));
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSlug()).isEqualTo("poplin-shirt");
    }

    // ---------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------

    private Category category(String name, String slug) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        return categoryRepository.save(c);
    }

    private Product product(String name, String slug, String sku, BigDecimal price,
                            int stock, ProductStatus status,
                            boolean featured, boolean newArrival, boolean bestseller,
                            Category category) {
        Product p = new Product();
        p.setName(name);
        p.setSlug(slug);
        p.setShortDescription("desc");
        p.setFullDescription("full desc");
        p.setSku(sku);
        p.setPrice(price);
        p.setStockQuantity(stock);
        p.setStatus(status);
        p.setFeatured(featured);
        p.setNewArrival(newArrival);
        p.setBestseller(bestseller);
        p.setCategory(category);
        return p;
    }
}
