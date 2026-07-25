package com.eazicut.api.reference;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import com.eazicut.api.categories.entity.Category;
import com.eazicut.api.categories.repository.CategoryRepository;
import com.eazicut.api.collections.entity.Collection;
import com.eazicut.api.collections.repository.CollectionRepository;

/**
 * Behavioural tests for {@link ReferenceDataSeeder} using in-memory fake
 * repositories.
 *
 * <p>Chose fakes over Mockito {@code @Mock} here because the seeder's
 * value is in its idempotency: "given a repository that already contains
 * some of the rows, only insert the missing ones." Fakes let us assert
 * on the full-cycle behaviour (first run creates 6, second run creates 0)
 * without the stubbing gymnastics that {@code given(existsBySlug(...))}
 * would require.
 */
@ExtendWith(MockitoExtension.class)
class ReferenceDataSeederTest {

    @Test
    @DisplayName("first run — seeds all 6 categories and all 6 collections into an empty repo")
    void firstRunSeedsEverything() {
        FakeCategoryRepository cats = new FakeCategoryRepository();
        FakeCollectionRepository cols = new FakeCollectionRepository();
        ReferenceDataSeeder seeder = new ReferenceDataSeeder(cats, cols);

        seeder.run(new DefaultApplicationArguments());

        assertThat(cats.saved).hasSize(6);
        assertThat(cats.saved).extracting(Category::getSlug)
                .containsExactlyInAnyOrder("suits", "shirts", "trousers", "outerwear", "native", "accessories");

        assertThat(cols.saved).hasSize(6);
        assertThat(cols.saved).extracting(Collection::getSlug)
                .containsExactlyInAnyOrder(
                        "the-onyx-bespoke", "ivory-wedding", "lagos-heritage",
                        "the-essentials", "the-noir-tuxedo", "diaspora");
    }

    @Test
    @DisplayName("second run — is a no-op; nothing is re-inserted")
    void secondRunIsIdempotent() {
        FakeCategoryRepository cats = new FakeCategoryRepository();
        FakeCollectionRepository cols = new FakeCollectionRepository();
        ReferenceDataSeeder seeder = new ReferenceDataSeeder(cats, cols);

        seeder.run(new DefaultApplicationArguments()); // populate
        int catsAfterFirst = cats.saved.size();
        int colsAfterFirst = cols.saved.size();

        seeder.run(new DefaultApplicationArguments()); // must be no-op

        assertThat(cats.saved).hasSize(catsAfterFirst);
        assertThat(cols.saved).hasSize(colsAfterFirst);
    }

    @Test
    @DisplayName("partial pre-existing state — seeds only the missing rows")
    void seedsOnlyMissing() {
        FakeCategoryRepository cats = new FakeCategoryRepository();
        FakeCollectionRepository cols = new FakeCollectionRepository();

        // Simulate one category and one collection already present with the
        // canonical slug but a different description — must NOT be overwritten.
        Category preexisting = new Category();
        preexisting.setName("Suits");
        preexisting.setSlug("suits");
        preexisting.setDescription("Original description — must survive.");
        cats.saved.add(preexisting);

        Collection preCol = new Collection();
        preCol.setName("The Onyx Bespoke");
        preCol.setSlug("the-onyx-bespoke");
        preCol.setDescription("Original — must survive.");
        cols.saved.add(preCol);

        new ReferenceDataSeeder(cats, cols).run(new DefaultApplicationArguments());

        assertThat(cats.saved).hasSize(6);
        assertThat(cats.findBySlugDescription("suits")).isEqualTo("Original description — must survive.");
        assertThat(cols.saved).hasSize(6);
        assertThat(cols.findBySlugDescription("the-onyx-bespoke")).isEqualTo("Original — must survive.");
    }

    // -----------------------------------------------------------------------
    // Fake repositories — implement only the methods the seeder touches
    // (existsBySlug, save). The unimplemented default-methods on the
    // JpaRepository interface aren't invoked so they're safe to leave.
    // -----------------------------------------------------------------------

    static class FakeCategoryRepository extends AbstractFakeRepo<Category>
            implements CategoryRepository {
        @Override String slugOf(Category c) { return c.getSlug(); }
        @Override String nameOf(Category c) { return c.getName(); }
    }

    static class FakeCollectionRepository extends AbstractFakeRepo<Collection>
            implements CollectionRepository {
        @Override String slugOf(Collection c) { return c.getSlug(); }
        @Override String nameOf(Collection c) { return c.getName(); }
    }

    /**
     * Minimal fake that supports the two methods the seeder actually uses.
     * All other JpaRepository methods throw so any regression to a wider
     * surface is caught immediately.
     */
    abstract static class AbstractFakeRepo<T> {
        final List<T> saved = new java.util.ArrayList<>();

        abstract String slugOf(T entity);
        abstract String nameOf(T entity);

        public boolean existsBySlug(String slug) {
            return saved.stream().anyMatch(e -> slug.equals(slugOf(e)));
        }

        public boolean existsByNameIgnoreCase(String name) {
            return saved.stream().anyMatch(e -> name.equalsIgnoreCase(nameOf(e)));
        }

        public java.util.Optional<T> findBySlug(String slug) {
            return saved.stream().filter(e -> slug.equals(slugOf(e))).findFirst();
        }

        public <S extends T> S save(S entity) {
            saved.add(entity);
            return entity;
        }

        String findBySlugDescription(String slug) {
            T e = saved.stream().filter(x -> slug.equals(slugOf(x))).findFirst().orElseThrow();
            if (e instanceof Category c) return c.getDescription();
            if (e instanceof Collection c) return c.getDescription();
            throw new IllegalStateException();
        }

        // ----- Every other JpaRepository method throws -----
        public <S extends T> List<S> saveAll(Iterable<S> e) { throw new UnsupportedOperationException(); }
        public java.util.Optional<T> findById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        public boolean existsById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        public List<T> findAll() { throw new UnsupportedOperationException(); }
        public List<T> findAllById(Iterable<java.util.UUID> ids) { throw new UnsupportedOperationException(); }
        public long count() { throw new UnsupportedOperationException(); }
        public void deleteById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        public void delete(T entity) { throw new UnsupportedOperationException(); }
        public void deleteAllById(Iterable<? extends java.util.UUID> ids) { throw new UnsupportedOperationException(); }
        public void deleteAll(Iterable<? extends T> entities) { throw new UnsupportedOperationException(); }
        public void deleteAll() { throw new UnsupportedOperationException(); }
        public <S extends T> S saveAndFlush(S entity) { throw new UnsupportedOperationException(); }
        public <S extends T> List<S> saveAllAndFlush(Iterable<S> entities) { throw new UnsupportedOperationException(); }
        public void deleteAllInBatch(Iterable<T> entities) { throw new UnsupportedOperationException(); }
        public void deleteAllByIdInBatch(Iterable<java.util.UUID> ids) { throw new UnsupportedOperationException(); }
        public void deleteAllInBatch() { throw new UnsupportedOperationException(); }
        public T getOne(java.util.UUID id) { throw new UnsupportedOperationException(); }
        public T getById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        public T getReferenceById(java.util.UUID id) { throw new UnsupportedOperationException(); }
        public <S extends T, R> R findBy(org.springframework.data.domain.Example<S> ex, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> f) { throw new UnsupportedOperationException(); }
        public <S extends T> java.util.Optional<S> findOne(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        public <S extends T> List<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
        public <S extends T> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> ex, org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        public <S extends T> long count(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        public <S extends T> boolean exists(org.springframework.data.domain.Example<S> ex) { throw new UnsupportedOperationException(); }
        public List<T> findAll(org.springframework.data.domain.Sort s) { throw new UnsupportedOperationException(); }
        public org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable p) { throw new UnsupportedOperationException(); }
        public void flush() { throw new UnsupportedOperationException(); }
    }
}
