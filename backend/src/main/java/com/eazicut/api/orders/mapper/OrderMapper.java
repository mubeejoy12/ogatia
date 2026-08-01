package com.eazicut.api.orders.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.eazicut.api.orders.dto.OrderItemResponse;
import com.eazicut.api.orders.dto.OrderResponse;
import com.eazicut.api.orders.dto.ShippingAddressDto;
import com.eazicut.api.orders.entity.Order;
import com.eazicut.api.orders.entity.OrderItem;
import com.eazicut.api.orders.entity.ShippingAddress;

/**
 * MapStruct mapper for the Order read model.
 *
 * <p>One direction only (entity → response). Order entities are
 * constructed by the create-order service (Stage 2) explicitly so
 * snapshot columns, reference generation, and totals go through a
 * single intentional boundary.
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "shippingAddress", source = "shippingAddress")
    OrderResponse toResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem item);

    ShippingAddressDto toAddressDto(ShippingAddress address);
}
