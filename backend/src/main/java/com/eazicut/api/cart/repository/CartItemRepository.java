package com.eazicut.api.cart.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eazicut.api.cart.entity.CartItem;

/**
 * Spring Data repository for {@link CartItem}.
 *
 * <p>Two lookup axes matter to the service layer (arriving in later
 * B005 stages):
 *
 * <ul>
 *   <li>{@link #findByCartIdAndProductIdAndSize} — the
 *       "does this exact line already exist" probe for the add-item
 *       upsert.</li>
 *   <li>{@link #findByIdAndCartId} — the ownership-safe fetch for
 *       PATCH / DELETE by item id (the URL carries the item id but
 *       the service scopes the query to the caller's cart, so a
 *       customer can never touch another customer's line).</li>
 * </ul>
 */
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    Optional<CartItem> findByCartIdAndProductIdAndSize(UUID cartId, UUID productId, String size);

    Optional<CartItem> findByIdAndCartId(UUID id, UUID cartId);
}
