package com.eazicut.api.reference;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.eazicut.api.categories.entity.Category;
import com.eazicut.api.categories.repository.CategoryRepository;
import com.eazicut.api.collections.entity.Collection;
import com.eazicut.api.collections.repository.CollectionRepository;

import lombok.RequiredArgsConstructor;

/**
 * Dev-only, idempotent reference-data seed.
 *
 * <p><strong>What it does.</strong> Ensures the six categories and six
 * collections the current storefront taxonomy assumes exist on every
 * dev boot. Slugs are the exact values the frontend derives:
 *
 * <ul>
 *   <li>Categories — {@link com.eazicut.api.categories.entity.Category} —
 *       {@code suits, shirts, trousers, outerwear, native, accessories}
 *       (each is {@code name.toLowerCase().replace(/\s+/g, "-")} per
 *       {@code CATEGORY_SLUG} in {@code src/features/shop/backendFilter.ts}).</li>
 *   <li>Collections — {@link com.eazicut.api.collections.entity.Collection} —
 *       {@code the-onyx-bespoke, ivory-wedding, lagos-heritage,
 *       the-essentials, the-noir-tuxedo, diaspora} (verbatim from
 *       {@code src/lib/data/collections.ts}).</li>
 * </ul>
 *
 * <p><strong>Why not Flyway.</strong> Flyway is the schema authority
 * ({@code hibernate.ddl-auto=validate} in every profile) and its
 * migrations are immutable. Reference data may drift as the atelier
 * curates the taxonomy; putting mutable content in an immutable schema
 * migration is a footgun. This seeder runs after the app context is
 * ready and speaks the same domain model as the rest of the app.
 *
 * <p><strong>Why dev-only.</strong> {@link Profile @Profile("dev")}
 * excludes prod and test — production reference data is loaded via the
 * admin API (or, once auth is real, a controlled ops job), never
 * silently on boot. Tests seed their own fixtures.
 *
 * <p><strong>Idempotency.</strong> Each row is guarded by
 * {@code existsBySlug}; re-running the seeder is a no-op on already-
 * seeded rows. Concurrent boots (two dev instances hitting the same
 * schema at once) still race on the check-then-insert, but the DB-level
 * unique constraints on {@code slug} and {@code name_lower} are the
 * backstop — the loser's INSERT throws {@code DataIntegrityViolationException}
 * and the transaction rolls back, leaving the schema in a valid state.
 *
 * <p><strong>Transaction shape.</strong> Categories and collections are
 * seeded in one transaction each (via the {@code @Transactional} method
 * calls) so a partial failure inside one entity kind rolls back only
 * that kind — the other still gets its chance to seed.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class ReferenceDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataSeeder.class);

    /**
     * The six categories the frontend URL taxonomy expects. Descriptions
     * are deliberately terse — the atelier will curate the copy over time
     * via the admin PUT endpoint, and this seeder never overwrites an
     * existing row.
     */
    private static final List<SeedRow> CATEGORIES = List.of(
            new SeedRow("Suits",       "suits",       "Tailored suits — two-piece, three-piece, bespoke."),
            new SeedRow("Shirts",      "shirts",      "Formal and casual shirts in Italian cotton."),
            new SeedRow("Trousers",    "trousers",    "Wool, cotton, and travel-weight trousers."),
            new SeedRow("Outerwear",   "outerwear",   "Overcoats, blazers, and travel jackets."),
            new SeedRow("Native",      "native",      "Agbada, kaftan, and Aso Oke pieces."),
            new SeedRow("Accessories", "accessories", "Belts, pocket squares, and cufflinks.")
    );

    /**
     * The six storefront collections. Names, slugs, and taglines mirror
     * {@code src/lib/data/collections.ts} exactly so the URL contract
     * {@code /shop?collection=<slug>} works out of the box against a
     * fresh dev DB.
     */
    private static final List<SeedRow> COLLECTIONS = List.of(
            new SeedRow("The Onyx Bespoke", "the-onyx-bespoke", "Black wool, cut clean."),
            new SeedRow("Ivory Wedding",    "ivory-wedding",    "For the day that photographs forever."),
            new SeedRow("Lagos Heritage",   "lagos-heritage",   "Heritage, drawn with a modern hand."),
            new SeedRow("The Essentials",   "the-essentials",   "The wardrobe underneath the wardrobe."),
            new SeedRow("The Noir Tuxedo",  "the-noir-tuxedo",  "The last suit you'll ever need to buy."),
            new SeedRow("Diaspora",         "diaspora",         "Tailored to travel.")
    );

    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;

    @Override
    public void run(ApplicationArguments args) {
        int cats = seedCategories();
        int cols = seedCollections();
        log.info("[reference-data-seed] complete — categories: {} created, {} already present · " +
                        "collections: {} created, {} already present",
                cats, CATEGORIES.size() - cats,
                cols, COLLECTIONS.size() - cols);
    }

    @Transactional
    int seedCategories() {
        int created = 0;
        for (SeedRow row : CATEGORIES) {
            if (categoryRepository.existsBySlug(row.slug())) {
                continue;
            }
            Category c = new Category();
            c.setName(row.name());
            c.setSlug(row.slug());
            c.setDescription(row.description());
            categoryRepository.save(c);
            created++;
            log.info("[reference-data-seed] created category: {} ({})", row.name(), row.slug());
        }
        return created;
    }

    @Transactional
    int seedCollections() {
        int created = 0;
        for (SeedRow row : COLLECTIONS) {
            if (collectionRepository.existsBySlug(row.slug())) {
                continue;
            }
            Collection c = new Collection();
            c.setName(row.name());
            c.setSlug(row.slug());
            c.setDescription(row.description());
            collectionRepository.save(c);
            created++;
            log.info("[reference-data-seed] created collection: {} ({})", row.name(), row.slug());
        }
        return created;
    }

    /**
     * Simple triplet used to keep the seed lists readable inline. Not a
     * DTO — the mapper isn't involved. Package-private on purpose.
     */
    record SeedRow(String name, String slug, String description) {}
}
