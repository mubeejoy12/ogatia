package com.eazicut.api.collections.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eazicut.api.collections.entity.Collection;

/**
 * Spring Data repository for {@link Collection}.
 *
 * <p>{@link JpaRepository} contributes {@code findAll(Pageable)},
 * {@code findById}, {@code save}, {@code deleteById}, {@code existsById}
 * out of the box — that covers the standalone CRUD surface. The extra
 * methods here support the service's duplicate probes:
 *
 * <ul>
 *   <li>{@link #findBySlug(String)} — slug is the public identifier the
 *       storefront uses (product filter axis, canonical URLs).</li>
 *   <li>{@link #existsBySlug(String)} — cheap uniqueness probe before
 *       insert / on slug change.</li>
 *   <li>{@link #existsByNameIgnoreCase(String)} — case-insensitive name
 *       uniqueness. Backed by a unique index on the
 *       {@code Collection.name_lower} column (V3 migration) so
 *       uniqueness holds under concurrent writes and portably across
 *       H2 and PostgreSQL.</li>
 * </ul>
 */
public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    Optional<Collection> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);
}
