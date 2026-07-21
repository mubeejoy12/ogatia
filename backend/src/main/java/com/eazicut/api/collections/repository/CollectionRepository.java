package com.eazicut.api.collections.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eazicut.api.collections.entity.Collection;

/**
 * Spring Data repository for {@link Collection}.
 *
 * <p>Minimal API surface — the future Collections feature ticket extends
 * this with search / paging / featured-collection queries.
 */
public interface CollectionRepository extends JpaRepository<Collection, UUID> {

    Optional<Collection> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
