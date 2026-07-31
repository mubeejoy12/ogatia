package com.eazicut.api.cart.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import com.eazicut.api.cart.dto.CartItemResponse;
import com.eazicut.api.cart.dto.CartItemSnapshotDto;
import com.eazicut.api.cart.entity.CartItem;
import com.eazicut.api.products.entity.Product;
import com.eazicut.api.products.entity.ProductStatus;

/**
 * MapStruct mapper for the cart read model.
 *
 * <p>Only writes one direction (entity → response). Cart entities are
 * built by the service layer explicitly so snapshot columns, quantity
 * clamps, and add-time behaviour go through a single intentional
 * boundary.
 *
 * <p><strong>Live fields</strong> — {@code currentPrice} and
 * {@code available} on the item response come from the associated
 * {@link Product}, not from the snapshot columns. The item is
 * {@code available} when the product is not soft-deleted, its status
 * is {@code ACTIVE} or {@code OUT_OF_STOCK} (we still show the line
 * for OoS so the customer can decide), stock is positive, and the
 * chosen size is still on offer. Stage 3 refines the classification
 * into per-issue codes; the boolean here is the coarse union.
 */
@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productSlug", source = "product.slug")
    @Mapping(target = "snapshot", source = ".", qualifiedByName = "snapshotOf")
    @Mapping(target = "currentPrice", source = "product.price")
    @Mapping(target = "available", source = ".", qualifiedByName = "computeAvailable")
    CartItemResponse toItemResponse(CartItem item);

    @Named("snapshotOf")
    default CartItemSnapshotDto snapshotOf(CartItem item) {
        return new CartItemSnapshotDto(
                item.getSnapshotName(),
                item.getSnapshotSlug(),
                item.getSnapshotPrice(),
                item.getSnapshotCurrency(),
                item.getSnapshotImageUrl()
        );
    }

    @Named("computeAvailable")
    default boolean computeAvailable(CartItem item) {
        Product product = item.getProduct();
        if (product == null) return false;
        ProductStatus status = product.getStatus();
        boolean statusOk = status == ProductStatus.ACTIVE;
        boolean stockOk = product.getStockQuantity() >= item.getQuantity();
        boolean sizeOk = product.getAvailableSizes() != null
                && product.getAvailableSizes().contains(item.getSize());
        return statusOk && stockOk && sizeOk;
    }
}
