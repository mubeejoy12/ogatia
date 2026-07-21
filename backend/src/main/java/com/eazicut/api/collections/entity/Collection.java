package com.eazicut.api.collections.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

import com.eazicut.api.common.entity.AbstractAuditableEntity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minimal Collection entity.
 *
 * <p>The name intentionally shadows {@link java.util.Collection java.util.Collection};
 * consumers that need both must import the util one by FQN. Domain vocabulary
 * ("Onyx Bespoke Collection", "Ivory Wedding Collection") wins over Java-stdlib
 * neatness.
 *
 * <p>Same rationale as {@link com.eazicut.api.categories.entity.Category Category}:
 * only the entity + repository ships in this ticket. A future Collections feature
 * ticket layers a controller/service on top.
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

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;
}
