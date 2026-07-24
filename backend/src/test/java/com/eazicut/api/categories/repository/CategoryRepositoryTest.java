package com.eazicut.api.categories.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.eazicut.api.categories.entity.Category;

/**
 * Repository-slice tests for {@link CategoryRepository}.
 *
 * <p>{@link DataJpaTest} boots only the JPA layer against H2. The slice's
 * default {@code ddl-auto=create-drop} is overridden to {@code validate}
 * so Flyway is the schema authority — the same migrations that build
 * prod's schema build the test schema, including V2's functional unique
 * index on {@code LOWER(name)}.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
class CategoryRepositoryTest {

    @Autowired private CategoryRepository categoryRepository;

    private Category persist(String name, String slug) {
        Category c = new Category();
        c.setName(name);
        c.setSlug(slug);
        return categoryRepository.saveAndFlush(c);
    }

    @Test
    @DisplayName("findBySlug — returns the persisted row")
    void findBySlug() {
        persist("Suits", "suits");
        assertThat(categoryRepository.findBySlug("suits")).isPresent()
                .get().extracting(Category::getName).isEqualTo("Suits");
    }

    @Test
    @DisplayName("existsBySlug — true for present slug, false for absent")
    void existsBySlug() {
        persist("Suits", "suits");
        assertThat(categoryRepository.existsBySlug("suits")).isTrue();
        assertThat(categoryRepository.existsBySlug("shirts")).isFalse();
    }

    @Test
    @DisplayName("existsByNameIgnoreCase — matches regardless of case")
    void existsByNameIgnoreCase() {
        persist("Suits", "suits");
        assertThat(categoryRepository.existsByNameIgnoreCase("Suits")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("suits")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("SUITS")).isTrue();
        assertThat(categoryRepository.existsByNameIgnoreCase("Shirts")).isFalse();
    }

    @Test
    @DisplayName("V2 functional unique index — insert with duplicate case-different name is rejected")
    void uniqueIndexOnLowerName() {
        persist("Suits", "suits");
        assertThatThrownBy(() -> persist("SUITS", "another-suits"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("V1 unique constraint on slug — insert with duplicate slug is rejected")
    void uniqueConstraintOnSlug() {
        persist("Suits", "suits");
        assertThatThrownBy(() -> persist("Different Name", "suits"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
