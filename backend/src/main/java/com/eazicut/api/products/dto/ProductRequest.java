package com.eazicut.api.products.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.eazicut.api.products.entity.ProductStatus;

/**
 * Create / update payload for a {@link com.eazicut.api.products.entity.Product Product}.
 *
 * <p>PUT semantics — a full replacement of the resource. A partial PATCH
 * would use a distinct DTO (which we introduce only if the endpoint is added).
 *
 * <p>Nullable fields carry sensible server-side defaults applied by the
 * service layer:
 * <ul>
 *   <li>{@code brand} → {@code "Eazi Cut"}</li>
 *   <li>{@code currency} → {@code "NGN"}</li>
 *   <li>{@code status} → {@code DRAFT}</li>
 *   <li>flags default to {@code false}, {@code stockQuantity} to {@code 0}</li>
 * </ul>
 *
 * <p>Slug pattern accepts lowercase-alphanumeric words separated by single
 * hyphens — matching the frontend's URL convention exactly.
 */
public record ProductRequest(

        @NotBlank
        @Size(min = 2, max = 200)
        String name,

        @NotBlank
        @Size(min = 2, max = 220)
        @Pattern(
                regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must be lowercase words separated by single hyphens."
        )
        String slug,

        @NotBlank
        @Size(max = 500)
        String shortDescription,

        @NotBlank
        @Size(max = 5000)
        String fullDescription,

        @NotBlank
        @Size(max = 64)
        String sku,

        @Size(max = 100)
        String brand,

        @NotNull
        @DecimalMin(value = "0.01", message = "Price must be greater than zero.")
        @Digits(integer = 15, fraction = 4)
        BigDecimal price,

        @DecimalMin(value = "0.01", message = "Discount price must be greater than zero.")
        @Digits(integer = 15, fraction = 4)
        BigDecimal discountPrice,

        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO 4217 code.")
        String currency,

        ProductStatus status,

        @Min(value = 0, message = "Stock quantity cannot be negative.")
        Integer stockQuantity,

        Boolean featured,
        Boolean newArrival,
        Boolean bestseller,

        @Size(max = 100)
        String fabricType,

        @Size(max = 60)
        String color,

        Set<@NotBlank @Size(max = 32) String> availableSizes,

        Set<@NotBlank @Size(max = 64) String> tags,

        UUID categoryId,

        UUID collectionId,

        @Valid
        List<ProductImageDto> images
) {
}
