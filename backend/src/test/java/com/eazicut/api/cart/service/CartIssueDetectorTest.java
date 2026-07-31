package com.eazicut.api.cart.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.eazicut.api.cart.dto.CartIssueResponse;
import com.eazicut.api.cart.entity.Cart;
import com.eazicut.api.cart.entity.CartItem;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;

/**
 * Unit tests for {@link CartIssueDetector}. Pure logic — no Spring, no
 * DB, no mocks. Every test builds a hand-crafted cart with one or two
 * lines and asserts the emitted codes.
 */
class CartIssueDetectorTest {

    private final CartIssueDetector detector = new CartIssueDetector();

    @Test
    @DisplayName("no issues on a healthy line")
    void healthyLine() {
        Cart cart = cartWith(line(
                product("test", "Test", new BigDecimal("100"), ProductStatus.ACTIVE, 10, Set.of("L")),
                "L", 2, new BigDecimal("100")));
        assertThat(detector.detect(cart)).isEmpty();
    }

    @Test
    @DisplayName("price_changed — snapshot and current differ")
    void priceChanged() {
        Cart cart = cartWith(line(
                product("test", "Test", new BigDecimal("200"), ProductStatus.ACTIVE, 10, Set.of("L")),
                "L", 1, new BigDecimal("100")));
        List<CartIssueResponse> issues = detector.detect(cart);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).code()).isEqualTo("price_changed");
        assertThat(issues.get(0).message()).contains("100").contains("200");
    }

    @Test
    @DisplayName("price_changed — BigDecimal scale ('100' vs '100.00') is NOT flagged")
    void bigDecimalScaleIgnored() {
        Cart cart = cartWith(line(
                product("test", "Test", new BigDecimal("100.00"), ProductStatus.ACTIVE, 10, Set.of("L")),
                "L", 1, new BigDecimal("100")));
        // 100 vs 100.00 → equal by compareTo, not equal by equals()
        assertThat(detector.detect(cart)).isEmpty();
    }

    @Test
    @DisplayName("out_of_stock — status is OUT_OF_STOCK")
    void outOfStockStatus() {
        Cart cart = cartWith(line(
                product("test", "Test", new BigDecimal("100"), ProductStatus.OUT_OF_STOCK, 0, Set.of("L")),
                "L", 1, new BigDecimal("100")));
        List<CartIssueResponse> issues = detector.detect(cart);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).code()).isEqualTo("out_of_stock");
    }

    @Test
    @DisplayName("out_of_stock — status INACTIVE also fires (per ProductStatus Javadoc: temporarily withdrawn)")
    void inactiveFiresOutOfStock() {
        Cart cart = cartWith(line(
                product("test", "Test", new BigDecimal("100"), ProductStatus.INACTIVE, 5, Set.of("L")),
                "L", 1, new BigDecimal("100")));
        assertThat(detector.detect(cart))
                .extracting(CartIssueResponse::code)
                .containsExactly("out_of_stock");
    }

    @Test
    @DisplayName("out_of_stock — quantity exceeds current stock (partial reduce copy)")
    void quantityExceedsStock() {
        Cart cart = cartWith(line(
                product("test", "Test", new BigDecimal("100"), ProductStatus.ACTIVE, 2, Set.of("L")),
                "L", 5, new BigDecimal("100")));
        List<CartIssueResponse> issues = detector.detect(cart);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).code()).isEqualTo("out_of_stock");
        assertThat(issues.get(0).message()).contains("Only 2");
    }

    @Test
    @DisplayName("size_unavailable — size no longer offered")
    void sizeUnavailable() {
        Cart cart = cartWith(line(
                product("test", "Test", new BigDecimal("100"), ProductStatus.ACTIVE, 10, Set.of("M", "XL")),
                "L", 1, new BigDecimal("100")));
        List<CartIssueResponse> issues = detector.detect(cart);
        assertThat(issues).hasSize(1);
        assertThat(issues.get(0).code()).isEqualTo("size_unavailable");
    }

    @Test
    @DisplayName("product_removed — ARCHIVED status short-circuits (no follow-on codes)")
    void archivedShortCircuits() {
        // Even though the price + stock would also fire in isolation,
        // an ARCHIVED product only emits product_removed — nothing else
        // is actionable for a removed product.
        Product archived = product("test", "Test", new BigDecimal("999"), ProductStatus.ARCHIVED, 0, Set.of());
        Cart cart = cartWith(line(archived, "L", 5, new BigDecimal("100")));
        List<CartIssueResponse> issues = detector.detect(cart);
        assertThat(issues)
                .extracting(CartIssueResponse::code)
                .containsExactly("product_removed");
    }

    @Test
    @DisplayName("product_removed — null product (soft-deleted, join filtered) also produces the code")
    void nullProductFires() {
        CartItem item = new CartItem();
        item.setId(UUID.randomUUID());
        item.setSize("L");
        item.setQuantity(1);
        item.setSnapshotName("Ghost Piece");
        item.setSnapshotSlug("ghost");
        item.setSnapshotPrice(new BigDecimal("100"));
        item.setSnapshotCurrency("NGN");
        item.setAddedAt(Instant.now());
        item.setUpdatedAt(Instant.now());
        // NB: product left null intentionally

        Cart cart = cartWith(item);
        List<CartIssueResponse> issues = detector.detect(cart);
        assertThat(issues)
                .extracting(CartIssueResponse::code)
                .containsExactly("product_removed");
        assertThat(issues.get(0).message()).contains("Ghost Piece");
    }

    @Test
    @DisplayName("multiple lines — each line's issues are emitted independently")
    void multipleLines() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setItems(new ArrayList<>());
        cart.getItems().add(line(
                product("healthy", "Healthy", new BigDecimal("100"), ProductStatus.ACTIVE, 10, Set.of("L")),
                "L", 1, new BigDecimal("100")));
        cart.getItems().add(line(
                product("changed", "Price Changed", new BigDecimal("300"), ProductStatus.ACTIVE, 10, Set.of("L")),
                "L", 1, new BigDecimal("200")));
        cart.getItems().add(line(
                product("gone", "Retired", new BigDecimal("100"), ProductStatus.ARCHIVED, 10, Set.of("L")),
                "L", 1, new BigDecimal("100")));

        List<CartIssueResponse> issues = detector.detect(cart);
        assertThat(issues).extracting(CartIssueResponse::code)
                .containsExactly("price_changed", "product_removed");
    }

    @Test
    @DisplayName("multiple issues on ONE line — price_changed + size_unavailable + out_of_stock all fire together")
    void multipleIssuesPerLine() {
        Cart cart = cartWith(line(
                product("mess", "Mess", new BigDecimal("500"), ProductStatus.ACTIVE, 1, Set.of("M")),
                "L", 5, new BigDecimal("100")));
        assertThat(detector.detect(cart))
                .extracting(CartIssueResponse::code)
                .containsExactlyInAnyOrder("out_of_stock", "size_unavailable", "price_changed");
    }

    // ---------------------------------------------------------------------
    // Fixture helpers
    // ---------------------------------------------------------------------

    private Cart cartWith(CartItem... items) {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setItems(new ArrayList<>(List.of(items)));
        return cart;
    }

    private Product product(String slug, String name, BigDecimal price, ProductStatus status, int stock, Set<String> sizes) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setSlug(slug);
        p.setName(name);
        p.setPrice(price);
        p.setCurrency("NGN");
        p.setStatus(status);
        p.setStockQuantity(stock);
        p.setAvailableSizes(sizes);
        return p;
    }

    private CartItem line(Product product, String size, int qty, BigDecimal snapshotPrice) {
        CartItem i = new CartItem();
        i.setId(UUID.randomUUID());
        i.setProduct(product);
        i.setSize(size);
        i.setQuantity(qty);
        i.setSnapshotName(product.getName());
        i.setSnapshotSlug(product.getSlug());
        i.setSnapshotPrice(snapshotPrice);
        i.setSnapshotCurrency("NGN");
        i.setAddedAt(Instant.now());
        i.setUpdatedAt(Instant.now());
        return i;
    }
}
