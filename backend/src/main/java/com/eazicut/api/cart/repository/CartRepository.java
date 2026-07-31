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
 * <p>{@link #findByUserId} eagerly fetches {@code items} and each item's
 * {@code product} so a single query populates the whole read model —
 * avoids the N+1 that would otherwise fire when {@code CartMapper}
 * touches every line.
 */
public interface CartRepository extends JpaRepository<Cart, UUID> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);
}
