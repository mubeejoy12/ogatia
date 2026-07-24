package com.eazicut.api.products.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.eazicut.api.products.entity.Product;

/**
 * Spring Data repository for {@link Product}.
 *
 * <p>Extends {@link JpaSpecificationExecutor} so the service layer can
 * compose dynamic filters (Stage 5) without a bespoke query DSL.
 *
 * <p><strong>Fetch strategy.</strong> The detail-path queries
 * ({@link #findBySlug(String)}, {@link #findBySku(String)},
 * {@link #findDetailById(UUID)}) use {@link EntityGraph} to eagerly
 * fetch images + category + collection — preventing N+1 on PDP loads.
 * The list-path uses whatever the {@link JpaSpecificationExecutor}
 * query defines (Stage 5 tunes that).
 *
 * <p>Note: {@code findBySlug} / {@code findBySku} return {@link Optional}
 * respecting the {@code @SQLRestriction} on {@code Product} — soft-deleted
 * rows are invisible.
 */
public interface ProductRepository
        extends JpaRepository<Product, UUID>,
                JpaSpecificationExecutor<Product> {

    // ---------------------------------------------------------------------
    // Detail-path (eager)
    // ---------------------------------------------------------------------

    @EntityGraph(attributePaths = {"images", "category", "collection"})
    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"images", "category", "collection"})
    Optional<Product> findBySku(String sku);

    /**
     * Detail-path lookup by id, eagerly loading the child collections needed
     * to render a full product response.
     */
    @EntityGraph(attributePaths = {"images", "category", "collection"})
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id = :id")
    Optional<Product> findDetailById(UUID id);

    // ---------------------------------------------------------------------
    // Duplicate checks (used by the service layer to raise domain exceptions
    // before hitting a DB-level unique-constraint violation)
    // ---------------------------------------------------------------------

    boolean existsBySlug(String slug);

    boolean existsBySku(String sku);

    // ---------------------------------------------------------------------
    // Reference-integrity probes (used by Category / Collection services
    // to raise a domain-level "in use" exception before delete, instead
    // of relying on a generic DataIntegrityViolationException).
    // ---------------------------------------------------------------------

    long countByCategoryId(UUID categoryId);

    long countByCollectionId(UUID collectionId);
}
