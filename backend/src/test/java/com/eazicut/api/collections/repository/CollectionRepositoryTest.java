package com.eazicut.api.collections.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.eazicut.api.collections.entity.Collection;

/**
 * Repository-slice tests for {@link CollectionRepository}.
 *
 * <p>Direct parallel of {@code CategoryRepositoryTest}. {@link DataJpaTest}
 * boots only the JPA layer against H2; the slice's default
 * {@code ddl-auto=create-drop} is overridden to {@code validate} so
 * Flyway is the schema authority — the same migrations that build prod's
 * schema build the test schema, including V3's unique index on
 * {@code collections.name_lower}.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class CollectionRepositoryTest {

    @Autowired private CollectionRepository collectionRepository;

    private Collection persist(String name, String slug) {
        Collection c = new Collection();
        c.setName(name);
        c.setSlug(slug);
        return collectionRepository.saveAndFlush(c);
    }

    @Test
    @DisplayName("findBySlug — returns the persisted row")
    void findBySlug() {
        persist("The Onyx Bespoke", "the-onyx-bespoke");
        assertThat(collectionRepository.findBySlug("the-onyx-bespoke")).isPresent()
                .get().extracting(Collection::getName).isEqualTo("The Onyx Bespoke");
    }

    @Test
    @DisplayName("existsBySlug — true for present slug, false for absent")
    void existsBySlug() {
        persist("The Onyx Bespoke", "the-onyx-bespoke");
        assertThat(collectionRepository.existsBySlug("the-onyx-bespoke")).isTrue();
        assertThat(collectionRepository.existsBySlug("ivory-wedding")).isFalse();
    }

    @Test
    @DisplayName("existsByNameIgnoreCase — matches regardless of case")
    void existsByNameIgnoreCase() {
        persist("The Onyx Bespoke", "the-onyx-bespoke");
        assertThat(collectionRepository.existsByNameIgnoreCase("The Onyx Bespoke")).isTrue();
        assertThat(collectionRepository.existsByNameIgnoreCase("the onyx bespoke")).isTrue();
        assertThat(collectionRepository.existsByNameIgnoreCase("THE ONYX BESPOKE")).isTrue();
        assertThat(collectionRepository.existsByNameIgnoreCase("Ivory Wedding")).isFalse();
    }

    @Test
    @DisplayName("V3 unique index on name_lower — insert with case-different name is rejected")
    void uniqueIndexOnNameLower() {
        persist("The Onyx Bespoke", "the-onyx-bespoke");
        assertThatThrownBy(() -> persist("THE ONYX BESPOKE", "onyx-bespoke-v2"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V1 unique constraint on slug — insert with duplicate slug is rejected")
    void uniqueConstraintOnSlug() {
        persist("The Onyx Bespoke", "the-onyx-bespoke");
        assertThatThrownBy(() -> persist("Ivory Wedding", "the-onyx-bespoke"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
