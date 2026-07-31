package com.eazicut.api.cart.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

import com.eazicut.api.common.entity.AbstractAuditableEntity;
import com.eazicut.api.users.entity.User;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shopping cart — one per user (see {@code ux_cart_user} in V6).
 *
 * <p>Not soft-deleted. A "removed" cart is either an empty cart or
 * hard-deleted at account-deletion time via the users → carts
 * {@code ON DELETE CASCADE}.
 *
 * <p>Currency is captured on the cart, not per line — mixing currencies
 * in one bag isn't supported (products are all NGN at launch). A later
 * multi-currency ticket can lift this by moving currency onto the line.
 *
 * <p>Line ordering uses {@code added_at ASC} so the customer sees pieces
 * in the order they added them — the natural expectation of a bag.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(name = "carts")
public class Cart extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    /**
     * Owner. One-to-one with {@link User} — enforced by the
     * {@code ux_cart_user} unique index at the DB level; the JPA side
     * uses {@code @OneToOne} without {@code mappedBy} so the FK sits on
     * this table (not on {@code users}).
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * Lines in add order. Cascaded persist/merge/remove + orphanRemoval
     * so {@code cart.getItems().remove(item)} deletes the row on flush.
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("addedAt ASC")
    @BatchSize(size = 25)
    private List<CartItem> items = new ArrayList<>();
}
