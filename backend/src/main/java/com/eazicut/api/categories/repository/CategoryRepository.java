package com.eazicut.api.categories.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eazicut.api.categories.entity.Category;

/**
 * Spring Data repository for {@link Category}.
 *
 * <p>Minimal API surface — the full Category feature ticket will introduce
 * search/paging methods once a Categories controller lands.
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
