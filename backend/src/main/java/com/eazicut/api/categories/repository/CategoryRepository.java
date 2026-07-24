package com.eazicut.api.categories.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eazicut.api.categories.entity.Category;

/**
 * Spring Data repository for {@link Category}.
 *
 * <p>{@link JpaRepository} contributes {@code findAll(Pageable)},
 * {@code findById}, {@code save}, {@code deleteById}, {@code existsById}
 * out of the box — that covers the standalone CRUD surface. The extra
 * methods here support the service's duplicate probes:
 *
 * <ul>
 *   <li>{@link #findBySlug(String)} — slug is the public identifier
 *       consumers use (product filter axis, storefront URLs).</li>
 *   <li>{@link #existsBySlug(String)} — cheap uniqueness probe before
 *       insert / on slug change.</li>
 *   <li>{@link #existsByNameIgnoreCase(String)} — case-insensitive name
 *       uniqueness. Backed by a DB functional unique index on
 *       {@code LOWER(name)} (Stage 2 migration) so uniqueness holds
 *       under concurrent writes and portably across H2 and PostgreSQL.</li>
 * </ul>
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNameIgnoreCase(String name);
}
