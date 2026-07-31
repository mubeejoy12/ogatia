package com.eazicut.api.cart.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eazicut.api.cart.entity.Cart;

/**
 * Spring Data repository for {@link Cart}.
 *
 * <p>Deliberately narrow — the only lookup path is by owning user id.
 * No id-in-URL surface (see cart security model): the {@code CartService}
 * always resolves the user's cart from the authenticated principal,
 * never from a caller-supplied cart id, so there's no IDOR to defend.
 *
 * <p>{@link #findByUserId} eagerly fetches {@code items} and each
 * item's {@code product} — avoids the N+1 that would otherwise fire
 * when {@code CartMapper} touches every line.
 *
 * <p>{@code Product.availableSizes} is intentionally NOT in the graph:
 * adding it produces a Cartesian join with the items collection, so
 * every cart item appears N times where N is that product's number of
 * sizes. The @Batchsize on {@code Product} groups the follow-up size
 * queries into a single round-trip when the mapper touches them.
 * The touching must happen inside the same transaction that loaded
 * the cart — see {@code CartService.readForUser}.
 */
public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
