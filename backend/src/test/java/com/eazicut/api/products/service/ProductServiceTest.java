package com.eazicut.api.products.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eazicut.api.categories.entity.Category;
import com.eazicut.api.categories.repository.CategoryRepository;
import com.eazicut.api.collections.repository.CollectionRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.products.dto.ProductRequest;
import com.eazicut.api.products.dto.ProductResponse;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;
import com.eazicut.api.products.exception.DuplicateSkuException;
import com.eazicut.api.products.exception.DuplicateSlugException;
import com.eazicut.api.products.mapper.ProductMapper;
import com.eazicut.api.products.mapper.ProductMapperImpl;
import com.eazicut.api.products.repository.ProductRepository;

/**
 * Unit tests for {@link ProductService}.
 *
 * <p>Uses the generated {@link ProductMapperImpl} as-is (MapStruct emits a
 * plain class with no Spring dependencies) so mapping stays exercised;
 * repositories are mocked.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private CollectionRepository collectionRepository;

    private final ProductMapper mapper = new ProductMapperImpl();

    @InjectMocks private ProductService service;

    private ProductRequest baseRequest;

    @BeforeEach
    void setUp() {
        // @InjectMocks doesn't know about our concrete mapper — inject manually.
        service = new ProductService(productRepository, categoryRepository, collectionRepository, mapper);

        baseRequest = new ProductRequest(
                "The Onyx Two-Piece",
                "onyx-two-piece-suit",
                "Ink wool suit",
                "Loro Piana super-120s ink wool.",
                "EC-ONYX-2P",
                null, // brand — defaulted
                new BigDecimal("650000"),
                null, // discountPrice
                null, // currency — defaulted
                null, // status — defaulted to DRAFT
                5,
                null, null, null,   // flags — defaulted
                null, null,          // fabric, color
                null, null,          // sizes, tags
                null, null,          // categoryId, collectionId
                null                 // images
        );
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("create — persists and returns response when slug + sku are unique")
    void createHappyPath() {
        given(productRepository.existsBySlug(baseRequest.slug())).willReturn(false);
        given(productRepository.existsBySku(baseRequest.sku())).willReturn(false);
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        ProductResponse response = service.create(baseRequest);

        assertThat(response.name()).isEqualTo("The Onyx Two-Piece");
        assertThat(response.brand()).isEqualTo("Eazi Cut");       // default applied
        assertThat(response.currency()).isEqualTo("NGN");         // default applied
        assertThat(response.status()).isEqualTo(ProductStatus.DRAFT); // default applied
        assertThat(response.featured()).isFalse();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("create — throws DuplicateSlugException when slug is taken; save never called")
    void createDuplicateSlugRejected() {
        given(productRepository.existsBySlug(baseRequest.slug())).willReturn(true);

        assertThatThrownBy(() -> service.create(baseRequest))
                .isInstanceOf(DuplicateSlugException.class)
                .hasMessageContaining(baseRequest.slug());

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — throws DuplicateSkuException when SKU is taken; save never called")
    void createDuplicateSkuRejected() {
        given(productRepository.existsBySlug(anyString())).willReturn(false);
        given(productRepository.existsBySku(baseRequest.sku())).willReturn(true);

        assertThatThrownBy(() -> service.create(baseRequest))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining(baseRequest.sku());

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — unknown categoryId raises ResourceNotFoundException (404)")
    void createUnknownCategoryRejected() {
        UUID unknownCategoryId = UUID.randomUUID();
        ProductRequest withBadCategory = withCategory(baseRequest, unknownCategoryId);

        given(productRepository.existsBySlug(anyString())).willReturn(false);
        given(productRepository.existsBySku(anyString())).willReturn(false);
        given(categoryRepository.findById(unknownCategoryId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(withBadCategory))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — resolved category is attached to the persisted product")
    void createAttachesCategory() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Suits");
        category.setSlug("suits");

        ProductRequest withCategory = withCategory(baseRequest, category.getId());

        given(productRepository.existsBySlug(anyString())).willReturn(false);
        given(productRepository.existsBySku(anyString())).willReturn(false);
        given(categoryRepository.findById(category.getId())).willReturn(Optional.of(category));
        given(productRepository.save(any(Product.class))).willAnswer(inv -> inv.getArgument(0));

        ProductResponse response = service.create(withCategory);

        assertThat(response.category()).isNotNull();
        assertThat(response.category().slug()).isEqualTo("suits");
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("update — unknown id raises ResourceNotFoundException (404)")
    void updateNotFound() {
        UUID id = UUID.randomUUID();
        given(productRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, baseRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    // ---------------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("delete — unknown id raises ResourceNotFoundException (404); deleteById never called")
    void deleteNotFound() {
        UUID id = UUID.randomUUID();
        given(productRepository.existsById(id)).willReturn(false);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("delete — existing id delegates to repository.deleteById (soft delete)")
    void deleteHappyPath() {
        UUID id = UUID.randomUUID();
        given(productRepository.existsById(id)).willReturn(true);

        service.delete(id);

        verify(productRepository).deleteById(id);
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getBySlug — unknown slug raises ResourceNotFoundException (404)")
    void getBySlugNotFound() {
        given(productRepository.findBySlug("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nope");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static ProductRequest withCategory(ProductRequest r, UUID categoryId) {
        return new ProductRequest(
                r.name(), r.slug(), r.shortDescription(), r.fullDescription(), r.sku(),
                r.brand(), r.price(), r.discountPrice(), r.currency(), r.status(),
                r.stockQuantity(), r.featured(), r.newArrival(), r.bestseller(),
                r.fabricType(), r.color(), r.availableSizes(), r.tags(),
                categoryId, r.collectionId(), r.images()
        );
    }
}
