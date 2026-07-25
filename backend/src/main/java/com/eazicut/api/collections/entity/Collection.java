package com.eazicut.api.collections.entity;

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
 * Collection entity — seasonal / thematic groupings of products
 * ("The Onyx Bespoke", "Ivory Wedding", "Lagos Heritage", …).
 *
 * <p>The name intentionally shadows {@link java.util.Collection java.util.Collection};
 * consumers that need both must import the util one by FQN. Domain
 * vocabulary wins over Java-stdlib neatness.
 *
 * <p>Not soft-deleted: reference data whose loss must be prevented by FK
 * constraints, not by presence flags. B003's {@code CollectionService}
 * enforces this at the domain layer with an explicit in-use check that
 * raises {@code CollectionInUseException} (409) before delete.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Entity
@Table(
        name = "collections",
        indexes = {
                @Index(name = "idx_collection_slug", columnList = "slug", unique = true),
                @Index(name = "idx_collection_name", columnList = "name")
        }
)
@BatchSize(size = 25)
public class Collection extends AbstractAuditableEntity {

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
     * <p>Backs the {@code ux_collection_name_lower} unique index (V3
     * migration) which enforces case-insensitive name uniqueness at the
     * database layer. See the V3 migration file for why this is a plain
     * column populated in Java rather than a functional index or a
     * DB-generated column (H2 v2 and PostgreSQL disagree on the syntax
     * for both alternatives). Direct parallel of {@code Category.nameLower}.
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
