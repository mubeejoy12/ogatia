package com.eazicut.api.collections.service;

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

import com.eazicut.api.collections.dto.CollectionRequest;
import com.eazicut.api.collections.dto.CollectionResponse;
import com.eazicut.api.collections.entity.Collection;
import com.eazicut.api.collections.exception.CollectionInUseException;
import com.eazicut.api.collections.exception.DuplicateCollectionNameException;
import com.eazicut.api.collections.exception.DuplicateCollectionSlugException;
import com.eazicut.api.collections.mapper.CollectionMapper;
import com.eazicut.api.collections.mapper.CollectionMapperImpl;
import com.eazicut.api.collections.repository.CollectionRepository;
import com.eazicut.api.common.exception.ResourceNotFoundException;
import com.eazicut.api.products.repository.ProductRepository;

/**
 * Unit tests for {@link CollectionService}.
 *
 * <p>Direct parallel of {@code CategoryServiceTest} — the two services are
 * mechanical mirrors so the tests are too. Uses the generated
 * {@link CollectionMapperImpl} directly (MapStruct emits a plain class
 * with no Spring dependencies) so mapping stays exercised alongside the
 * service logic; repositories are mocked.
 */
@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock private CollectionRepository collectionRepository;
    @Mock private ProductRepository productRepository;

    private final CollectionMapper mapper = new CollectionMapperImpl();

    private CollectionService service;

    private CollectionRequest baseRequest;

    @BeforeEach
    void setUp() {
        service = new CollectionService(collectionRepository, productRepository, mapper);
        baseRequest = new CollectionRequest("The Onyx Bespoke", "the-onyx-bespoke", "Ink wool bespoke.");
    }

    // ---------------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("create — persists and returns response when slug + name are unique")
    void createHappyPath() {
        given(collectionRepository.existsBySlug("the-onyx-bespoke")).willReturn(false);
        given(collectionRepository.existsByNameIgnoreCase("The Onyx Bespoke")).willReturn(false);
        given(collectionRepository.save(any(Collection.class))).willAnswer(inv -> inv.getArgument(0));

        CollectionResponse response = service.create(baseRequest);

        assertThat(response.name()).isEqualTo("The Onyx Bespoke");
        assertThat(response.slug()).isEqualTo("the-onyx-bespoke");
        verify(collectionRepository).save(any(Collection.class));
    }

    @Test
    @DisplayName("create — throws DuplicateCollectionSlugException when slug is taken; save never called")
    void createDuplicateSlugRejected() {
        given(collectionRepository.existsBySlug("the-onyx-bespoke")).willReturn(true);

        assertThatThrownBy(() -> service.create(baseRequest))
                .isInstanceOf(DuplicateCollectionSlugException.class)
                .hasMessageContaining("the-onyx-bespoke");

        verify(collectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — throws DuplicateCollectionNameException when name (case-insensitive) is taken")
    void createDuplicateNameRejected() {
        given(collectionRepository.existsBySlug(anyString())).willReturn(false);
        given(collectionRepository.existsByNameIgnoreCase("The Onyx Bespoke")).willReturn(true);

        assertThatThrownBy(() -> service.create(baseRequest))
                .isInstanceOf(DuplicateCollectionNameException.class)
                .hasMessageContaining("The Onyx Bespoke");

        verify(collectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("create — normalises name (trim + collapse whitespace) before probing and persisting")
    void createNormalisesName() {
        CollectionRequest messy = new CollectionRequest("  The   Onyx  Bespoke ", "the-onyx-bespoke", null);
        given(collectionRepository.existsBySlug("the-onyx-bespoke")).willReturn(false);
        given(collectionRepository.existsByNameIgnoreCase("The Onyx Bespoke")).willReturn(false);
        given(collectionRepository.save(any(Collection.class))).willAnswer(inv -> inv.getArgument(0));

        CollectionResponse response = service.create(messy);

        verify(collectionRepository).existsByNameIgnoreCase("The Onyx Bespoke");
        assertThat(response.name()).isEqualTo("The Onyx Bespoke");
    }

    // ---------------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("update — unknown id raises ResourceNotFoundException (404)")
    void updateNotFound() {
        UUID id = UUID.randomUUID();
        given(collectionRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, baseRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Collection");
    }

    @Test
    @DisplayName("update — no-op change does not re-probe uniqueness (would false-positive on self)")
    void updateNoOpSkipsProbes() {
        UUID id = UUID.randomUUID();
        Collection existing = new Collection();
        existing.setId(id);
        existing.setName("The Onyx Bespoke");
        existing.setSlug("the-onyx-bespoke");
        given(collectionRepository.findById(id)).willReturn(Optional.of(existing));

        service.update(id, baseRequest);

        verify(collectionRepository, never()).existsBySlug(anyString());
        verify(collectionRepository, never()).existsByNameIgnoreCase(anyString());
    }

    @Test
    @DisplayName("update — slug change probes slug availability only")
    void updateSlugChangeProbes() {
        UUID id = UUID.randomUUID();
        Collection existing = new Collection();
        existing.setId(id);
        existing.setName("The Onyx Bespoke");
        existing.setSlug("the-onyx-bespoke");
        given(collectionRepository.findById(id)).willReturn(Optional.of(existing));
        given(collectionRepository.existsBySlug("onyx-bespoke-v2")).willReturn(false);

        CollectionRequest changed = new CollectionRequest("The Onyx Bespoke", "onyx-bespoke-v2", null);
        service.update(id, changed);

        verify(collectionRepository, times(1)).existsBySlug("onyx-bespoke-v2");
        verify(collectionRepository, never()).existsByNameIgnoreCase(anyString());
    }

    // ---------------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("delete — refuses when products reference the collection (409 with count)")
    void deleteBlockedByProducts() {
        UUID id = UUID.randomUUID();
        Collection existing = new Collection();
        existing.setId(id);
        existing.setName("The Onyx Bespoke");
        existing.setSlug("the-onyx-bespoke");
        given(collectionRepository.findById(id)).willReturn(Optional.of(existing));
        given(productRepository.countByCollectionId(id)).willReturn(3L);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(CollectionInUseException.class)
                .hasMessageContaining("the-onyx-bespoke")
                .hasMessageContaining("3");

        verify(collectionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("delete — happy path: no referring products → repository.delete invoked")
    void deleteHappyPath() {
        UUID id = UUID.randomUUID();
        Collection existing = new Collection();
        existing.setId(id);
        existing.setName("The Onyx Bespoke");
        existing.setSlug("the-onyx-bespoke");
        given(collectionRepository.findById(id)).willReturn(Optional.of(existing));
        given(productRepository.countByCollectionId(id)).willReturn(0L);

        service.delete(id);

        verify(collectionRepository).delete(existing);
    }

    @Test
    @DisplayName("delete — unknown id raises ResourceNotFoundException (404)")
    void deleteNotFound() {
        UUID id = UUID.randomUUID();
        given(collectionRepository.findById(id)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------------
    // Reads
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("getBySlug — unknown slug raises ResourceNotFoundException (404)")
    void getBySlugNotFound() {
        given(collectionRepository.findBySlug("nope")).willReturn(Optional.empty());

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
        assertThat(CollectionService.normaliseName("  The   Onyx  Bespoke ")).isEqualTo("The Onyx Bespoke");
        assertThat(CollectionService.normaliseName("Onyx Bespoke")).isEqualTo("Onyx Bespoke");
        assertThat(CollectionService.normaliseName("A\t\tB")).isEqualTo("A B");
        assertThat(CollectionService.normaliseName(null)).isNull();
    }
}
