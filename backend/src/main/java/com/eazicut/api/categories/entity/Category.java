package com.eazicut.api.categories.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.eazicut.api.common.entity.AbstractAuditableEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal Category entity.
 *
 * <p>Referenced by {@link com.eazicut.api.products.entity.Product Product}. This
 * ticket ships only the entity + repository so Product can hold a real
 * {@code @ManyToOne} relationship; a future Category feature ticket will add
 * controller, service, and richer domain fields (parent category, icon, ordering,
 * …) on top of this scaffold.
 *
 * <p>Not soft-deleted: reference data whose loss must be prevented by FK
 * constraints, not by presence flags.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_category_slug", columnList = "slug", unique = true),
                @Index(name = "idx_category_name", columnList = "name")
        }
)
public class Category extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;
}
