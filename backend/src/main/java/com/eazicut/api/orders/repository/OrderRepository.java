package com.eazicut.api.orders.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eazicut.api.orders.entity.Order;

/**
 * Spring Data repository for {@link Order}.
 *
 * <p><strong>Ownership-safe lookups.</strong> Detail-path methods
 * take a {@code userId} filter alongside the id / reference so a
 * caller can never fetch someone else's order — a fabricated or
 * enumerated id resolves to an empty {@link Optional} indistinguishable
 * from a real typo. Same pattern B005 used for
 * {@code CartItemRepository.findByIdAndCartId}.
 *
 * <p>Admin-facing lookups ({@link #findById}, {@link #findByReference})
 * are inherited or added below for later stages — the Stage 4 admin
 * status-transition endpoint needs them.
 *
 * <p>Detail-path methods use {@link EntityGraph} to eagerly fetch
 * {@code items} + {@code items.product} so the mapper doesn't fire
 * an N+1 on the response.
 */
public interface OrderRepository extends JpaRepository<Order, UUID> {

    // ------------------------------------------------------------------
    // Customer-facing (ownership-safe)
    // ------------------------------------------------------------------

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByReferenceAndUserId(String reference, UUID userId);

    Page<Order> findByUserId(UUID userId, Pageable pageable);

    // ------------------------------------------------------------------
    // Admin-facing (bypass ownership filter)
    // ------------------------------------------------------------------

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByReference(String reference);

    // ------------------------------------------------------------------
    // Duplicate probes (used by later stages)
    // ------------------------------------------------------------------

    boolean existsByReference(String reference);

    /**
     * Idempotency probe. Same (user, key) always resolves to the same
     * order; the create-order flow (Stage 2) returns the stored order
     * verbatim on replay. Composite scoping means keys cannot cross
     * accounts.
     */
    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Order> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}
