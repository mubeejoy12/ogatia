package com.eazicut.api.categories.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.eazicut.api.categories.dto.CategoryRequest;
import com.eazicut.api.categories.dto.CategoryResponse;
import com.eazicut.api.categories.entity.Category;
import com.eazicut.api.categories.exception.CategoryInUseException;
import com.eazicut.api.categories.exception.DuplicateCategoryNameException;
import com.eazicut.api.categories.exception.DuplicateCategorySlugException;
import com.eazicut.api.categories.mapper.CategoryMapper;
import com.eazicut.api.categories.mapper.CategoryMapperImpl;
import com.eazicut.api.categories.repository.CategoryRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.products.repository.ProductRepository;

/**
 * Unit tests for {@link CategoryService}.
 *
 * <p>Uses the generated {@link CategoryMapperImpl} directly (MapStruct
 * emits a plain class with no Spring dependencies) so mapping stays
 * exercised alongside the service logic; repositories are mocked.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private ProductRepository productRepository;

    private final CategoryMapper mapper = new CategoryMapperImpl();

    private CategoryService service;

    private CategoryRequest baseRequest;

    @BeforeEach
    void setUp() {
        service = new CategoryService(categoryRepository, productRepository, mapper);
        baseRequest = new CategoryRequest("Suits", "suits", "Tailored suits.");
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("create — persists and returns response when slug + name are unique")
    void createHappyPath() {
        given(categoryRepository.existsBySlug("suits")).willReturn(false);
        given(categoryRepository.existsByNameIgnoreCase("Suits")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = service.create(baseRequest);

        assertThat(response.name()).isEqualTo("Suits");
        assertThat(response.slug()).isEqualTo("suits");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("create — throws DuplicateCategorySlugException when slug is taken; save never called")
    void createDuplicateSlugRejected() {
        given(categoryRepository.existsBySlug("suits")).willReturn(true);

        assertThatThrownBy(() -> service.create(baseRequest))
                .isInstanceOf(DuplicateCategorySlugException.class)
                .hasMessageContaining("suits");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — throws DuplicateCategoryNameException when name (case-insensitive) is taken")
    void createDuplicateNameRejected() {
        given(categoryRepository.existsBySlug(anyString())).willReturn(false);
        given(categoryRepository.existsByNameIgnoreCase("Suits")).willReturn(true);

        assertThatThrownBy(() -> service.create(baseRequest))
                .isInstanceOf(DuplicateCategoryNameException.class)
                .hasMessageContaining("Suits");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — normalises name (trim + collapse whitespace) before probing and persisting")
    void createNormalisesName() {
        CategoryRequest messy = new CategoryRequest("  Formal   Suits ", "formal-suits", null);
        given(categoryRepository.existsBySlug("formal-suits")).willReturn(false);
        given(categoryRepository.existsByNameIgnoreCase("Formal Suits")).willReturn(false);
        given(categoryRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponse response = service.create(messy);

        // Probe must have been made against the normalised value, not the raw one.
        verify(categoryRepository).existsByNameIgnoreCase("Formal Suits");
        assertThat(response.name()).isEqualTo("Formal Suits");
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("update — unknown id raises ResourceNotFoundException (404)")
    void updateNotFound() {
        UUID id = UUID.randomUUID();
        given(categoryRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, baseRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category");
    }

    @Test
    @DisplayName("update — no-op change does not re-probe uniqueness (would false-positive on self)")
    void updateNoOpSkipsProbes() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        existing.setName("Suits");
        existing.setSlug("suits");
        given(categoryRepository.findById(id)).willReturn(Optional.of(existing));

        service.update(id, baseRequest);

        verify(categoryRepository, never()).existsBySlug(anyString());
        verify(categoryRepository, never()).existsByNameIgnoreCase(anyString());
    }

    @Test
    @DisplayName("update — slug change probes slug availability only")
    void updateSlugChangeProbes() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        existing.setName("Suits");
        existing.setSlug("suits");
        given(categoryRepository.findById(id)).willReturn(Optional.of(existing));
        given(categoryRepository.existsBySlug("formal-suits")).willReturn(false);

        CategoryRequest changed = new CategoryRequest("Suits", "formal-suits", null);
        service.update(id, changed);

        verify(categoryRepository, times(1)).existsBySlug("formal-suits");
        verify(categoryRepository, never()).existsByNameIgnoreCase(anyString());
    }

    // ---------------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("delete — refuses when products reference the category (409 with count)")
    void deleteBlockedByProducts() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        existing.setName("Suits");
        existing.setSlug("suits");
        given(categoryRepository.findById(id)).willReturn(Optional.of(existing));
        given(productRepository.countByCategoryId(id)).willReturn(3L);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(CategoryInUseException.class)
                .hasMessageContaining("suits")
                .hasMessageContaining("3");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete — happy path: no referring products → repository.delete invoked")
    void deleteHappyPath() {
        UUID id = UUID.randomUUID();
        Category existing = new Category();
        existing.setId(id);
        existing.setName("Suits");
        existing.setSlug("suits");
        given(categoryRepository.findById(id)).willReturn(Optional.of(existing));
        given(productRepository.countByCategoryId(id)).willReturn(0L);

        service.delete(id);

        verify(categoryRepository).delete(existing);
    }

    @Test
    @DisplayName("delete — unknown id raises ResourceNotFoundException (404)")
    void deleteNotFound() {
        UUID id = UUID.randomUUID();
        given(categoryRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getBySlug — unknown slug raises ResourceNotFoundException (404)")
    void getBySlugNotFound() {
        given(categoryRepository.findBySlug("nope")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBySlug("nope"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("nope");
    }

    // ---------------------------------------------------------------------
    // Name normalisation helper — unit-tested directly so the rule is
    // pinned even if the service is refactored later.
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("normaliseName — trims and collapses internal whitespace")
    void normaliseName() {
        assertThat(CategoryService.normaliseName("  Formal   Suits ")).isEqualTo("Formal Suits");
        assertThat(CategoryService.normaliseName("Suits")).isEqualTo("Suits");
        assertThat(CategoryService.normaliseName("A\t\tB")).isEqualTo("A B");
        assertThat(CategoryService.normaliseName(null)).isNull();
    }
}
