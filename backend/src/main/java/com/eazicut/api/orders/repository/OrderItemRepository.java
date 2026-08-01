package com.eazicut.api.orders.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eazicut.api.orders.entity.OrderItem;

/**
 * Spring Data repository for {@link OrderItem}.
 *
 * <p>Narrow surface — most order-item work goes through the parent
 * {@link com.eazicut.api.orders.entity.Order Order}'s cascade. The
 * couple of direct-access methods needed by later stages (e.g. count
 * by product for admin reports) can be added incrementally without
 * changing the entity model.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {
}
