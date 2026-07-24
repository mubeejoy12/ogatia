package com.eazicut.api.categories.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

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
@BatchSize(size = 25)
public class Category extends AbstractAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    /**
     * Lower-cased projection of {@link #name}, maintained by the
     * {@link #syncNameLower()} lifecycle callback below.
     *
     * <p>Backs the {@code ux_category_name_lower} unique index (V2
     * migration) which enforces case-insensitive name uniqueness at the
     * database layer. See the V2 migration file for why this is a plain
     * column populated in Java rather than a functional index or a
     * DB-generated column (H2 v2 and PostgreSQL disagree on the syntax
     * for both alternatives).
     */
    @Column(name = "name_lower", nullable = false, length = 120)
    private String nameLower;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @PrePersist
    @PreUpdate
    private void syncNameLower() {
        this.nameLower = name == null ? null : name.toLowerCase();
    }

    @Column(columnDefinition = "TEXT")
    private String description;
}
