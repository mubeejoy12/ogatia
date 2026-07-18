package com.eazicut.api.products.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.eazicut.api.common.entity.AbstractAuditableEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * A single image belonging to a {@link Product}.
 *
 * <p>Stored as a proper entity (not a simple {@code @ElementCollection<String>})
 * so each image carries its own metadata — {@code alt} text (accessibility),
 * {@code sortOrder} (gallery ordering), and {@code primary} flag (which shot
 * represents the product on cards and OG previews).
 *
 * <p>Reserved SQL keywords {@code order} and {@code primary} are mapped to
 * safe column names {@code sort_order} and {@code is_primary}.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@ToString(onlyExplicitlyIncluded = true)
@Entity
@Table(
        name = "product_images",
        indexes = {
                @Index(name = "idx_product_image_product_id", columnList = "product_id")
        }
)
public class ProductImage extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    /**
     * Back-reference to the owning product. LAZY so listing images for the
     * gallery does not accidentally hydrate the full product graph.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, foreignKey = @jakarta.persistence.ForeignKey(name = "fk_product_image_product"))
    private Product product;

    @Column(nullable = false, length = 2048)
    @ToString.Include
    private String url;

    @Column(length = 500)
    private String alt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "is_primary", nullable = false)
    private boolean primary = false;
}
