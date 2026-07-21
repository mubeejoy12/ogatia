# Eazi Cut — Backend API

Spring Boot 3.4 · Java 21 · Postgres 16 (H2 in dev) · Maven

## Run

```bash
cd backend
./mvnw spring-boot:run
```

Server boots on http://localhost:8080. Every endpoint is prefixed with `/api/v1`.

Sanity check:

```bash
curl http://localhost:8080/api/v1/health
# → {"status":"OK","service":"eazi-cut-api","profile":"dev","timestamp":"..."}
```

## Test

```bash
./mvnw test
```

## Build

```bash
./mvnw clean verify
```

Produces `target/eazi-cut-api-0.1.0-SNAPSHOT.jar` — runnable via `java -jar`.

## Profiles

| Profile | Database | CORS origin | Purpose |
|---|---|---|---|
| `dev` (default) | H2 in-memory (PostgreSQL compatibility mode) | `http://localhost:3000` | Local dev without Postgres install |
| `prod` | Postgres via env vars | Set via `ALLOWED_ORIGINS` | Production deployment |

## Database & Migrations

**Flyway is the single source of truth for the database schema in every
environment.** Hibernate runs in `ddl-auto: validate` mode — it only
cross-checks that the entity model matches the migrated schema and never
mutates the database itself.

### Where migrations live

```
backend/src/main/resources/db/migration/
└── V1__initial_schema.sql
```

### Naming convention

`V<version>__<snake_case_description>.sql`

- Version is an integer (or dotted, e.g. `2.1`). Never reuse a number.
- Two underscores between the version and the description.
- Description is lower-snake-case, short, human-scannable.

Good: `V2__add_orders_table.sql`, `V3__index_products_created_at.sql`
Bad: `V2_orders.sql` (single underscore), `V1__foo.sql` (already taken)

### How to add a new migration

Any change that alters the database schema — new entity, new column, new
index, changed nullability — must ship as a new migration file, otherwise
Hibernate `validate` refuses to start on next boot (fail-fast).

Workflow:

1. Modify the JPA entity (add field, add annotation, etc.).
2. Create a new `V<n>__<description>.sql` file with the corresponding DDL.
3. `./mvnw spring-boot:run` — Flyway applies the new migration to the local
   H2 database, Hibernate validates that the entity model now matches.
4. `./mvnw test` — the repository slice test picks up the same migration.
5. Commit both the entity change and the migration in the same commit.

**Do not** edit `V1__initial_schema.sql` (or any already-applied migration)
after it has been committed. Flyway detects checksum drift and refuses to
migrate; a "quick fix" in prod would be catastrophic.

### SQL compatibility

`V1__initial_schema.sql` is written to the intersection of PostgreSQL and
H2 (`MODE=PostgreSQL`) syntax, so the same file runs in dev + test (H2)
and prod (PostgreSQL). Keep future migrations to that intersection unless
you split into vendor-specific directories (e.g. `db/migration/postgresql/`).

Portability guidelines:

- ✅ `UUID`, `TEXT`, `VARCHAR(n)`, `NUMERIC(19,4)`, `BOOLEAN`, `TIMESTAMP(6) WITH TIME ZONE`, `INTEGER`
- ✅ Standard `CREATE TABLE`, `PRIMARY KEY`, `FOREIGN KEY`, `UNIQUE`, `CHECK`, `CREATE INDEX`
- ❌ Avoid: `gen_random_uuid()`, PG extensions (`pg_trgm`), GIN/GIST indexes, JSONB, ARRAY types
- ✅ UUIDs are always application-generated (`@GeneratedValue(strategy = GenerationType.UUID)`)
- ✅ Timestamps are always application-set (`@CreatedDate` / `@LastModifiedDate`)

### Running with a database

**Local dev (default)** — H2 in-memory. Flyway migrates on every boot,
Hibernate validates, nothing to configure:

```bash
./mvnw spring-boot:run
```

**Local Postgres (optional)** — swap the dev datasource block in
`application-dev.yml` to point at a local Postgres, or run with prod-style
env vars:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://localhost:5432/eazicut
export DATABASE_USERNAME=eazicut
export DATABASE_PASSWORD=…
export ALLOWED_ORIGINS=http://localhost:3000
./mvnw spring-boot:run
```

**Production** — Flyway runs on container startup against the real
Postgres. Deploy sequence:

1. Deploy the new JAR (with any new `V<n>__*.sql` files bundled inside).
2. On boot: Flyway inspects `flyway_schema_history`, applies any
   migrations with a version higher than the current, records them.
3. Hibernate then validates. If validate fails, the pod refuses to accept
   traffic — the deploy fails safely without touching data.

The migration is transactional per-file where the database supports it
(PostgreSQL does; H2 does for most DDL). A failed migration leaves the
database at the previous state.

### Legacy schema (one-off)

If you ever inherit a database that already has tables but no
`flyway_schema_history`, Flyway will refuse to run because
`baseline-on-migrate: false` (deliberate — protects against silent-skip
disasters). The correct response is a one-off manual baseline:

```bash
./mvnw flyway:baseline -Dflyway.url=… -Dflyway.user=… -Dflyway.password=…
```

Then the next deploy picks up cleanly.

### Inspecting migrations

**In dev (H2 console)** — visit `http://localhost:8080/api/v1/h2-console`
(when the dev profile is active), log in with the URL from
`application-dev.yml`, and run:

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

**Via Flyway plugin** — from `backend/`:

```bash
./mvnw flyway:info
```

Select with:

```bash
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```

## Environment variables

| Variable | Required in | Description |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | any | `dev` \| `prod`. Defaults to `dev`. |
| `PORT` | any | HTTP port. Defaults to `8080`. |
| `ALLOWED_ORIGINS` | any | Comma-separated origins allowed by CORS. Defaults to `http://localhost:3000`. |
| `DATABASE_URL` | prod | JDBC URL, e.g. `jdbc:postgresql://host:5432/eazicut`. |
| `DATABASE_USERNAME` | prod | Postgres user. |
| `DATABASE_PASSWORD` | prod | Postgres password. |
| `DB_POOL_SIZE` | prod | Hikari max pool size. Defaults to `10`. |

## Package structure

```
com.eazicut.api
├── EaziCutApiApplication      main entry point (@EnableJpaAuditing)
├── config                     cross-cutting configuration (CORS, security)
├── common
│   ├── controller             foundation controllers (Health)
│   ├── dto                    reusable envelopes (ApiResponse, ApiError, PagedResponse)
│   ├── entity                 AbstractAuditableEntity (createdAt / updatedAt base)
│   └── exception              custom exceptions + GlobalExceptionHandler
│                              (ResourceNotFound / BadRequest / Conflict base)
├── categories
│   ├── dto                    CategorySummary
│   ├── entity                 Category
│   └── repository             CategoryRepository
├── collections
│   ├── dto                    CollectionSummary
│   ├── entity                 Collection
│   └── repository             CollectionRepository
└── products
    ├── controller             ProductController
    ├── dto                    ProductRequest / Response / ImageDto / FilterCriteria
    ├── entity                 Product / ProductImage / ProductStatus
    ├── exception              DuplicateSlug / DuplicateSku (extend ConflictException)
    ├── mapper                 ProductMapper (MapStruct)
    ├── repository             ProductRepository (+ JpaSpecificationExecutor)
    ├── service                ProductService
    └── specification          ProductSpecifications
```

New feature packages follow the same `controller / service / repository / dto / entity / mapper` internal layout.

## Products API

All endpoints prefixed with `/api/v1` via `server.servlet.context-path`. Writes require `hasRole('ADMIN')`
(dev-only Basic auth: `admin` / `admin`).

| Method | Path | Auth | Body / Query | Returns |
|---|---|:-:|---|---|
| GET | `/products` | Public | `page`, `size`, `sort`, `search`, `category`, `collection`, `minPrice`, `maxPrice`, `featured`, `newArrival`, `bestseller`, `available`, `status` | `PagedResponse<ProductResponse>` |
| GET | `/products/featured` | Public | pagination | `PagedResponse<ProductResponse>` |
| GET | `/products/new-arrivals` | Public | pagination | `PagedResponse<ProductResponse>` |
| GET | `/products/bestsellers` | Public | pagination | `PagedResponse<ProductResponse>` |
| GET | `/products/{id}` | Public | — | `ApiResponse<ProductResponse>` |
| GET | `/products/slug/{slug}` | Public | — | `ApiResponse<ProductResponse>` |
| POST | `/products` | ADMIN | `ProductRequest` | 201 + `Location` + `ApiResponse<ProductResponse>` |
| PUT | `/products/{id}` | ADMIN | `ProductRequest` | `ApiResponse<ProductResponse>` |
| DELETE | `/products/{id}` | ADMIN | — | 204 No Content (soft delete) |

### Quick recipes (dev)

```bash
# Public list
curl http://localhost:8080/api/v1/products

# Featured only
curl http://localhost:8080/api/v1/products/featured

# Price-bounded list, sorted low → high
curl "http://localhost:8080/api/v1/products?minPrice=100000&maxPrice=700000&sort=price,asc"

# Keyword search (case-insensitive across name + description + sku)
curl "http://localhost:8080/api/v1/products?search=wool"

# Create a product (dev admin)
curl -u admin:admin -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
    "name": "The Onyx Two-Piece",
    "slug": "onyx-two-piece-suit",
    "shortDescription": "Ink wool two-piece",
    "fullDescription": "Loro Piana super-120s wool.",
    "sku": "EC-ONYX-2P-001",
    "price": 650000,
    "stockQuantity": 5,
    "status": "ACTIVE",
    "featured": true,
    "images": []
  }'
```

### Error format

Every non-2xx returns the uniform `ApiError` body:

- `400 validation_failed` with per-field `details[]` (Bean Validation)
- `400 invalid_parameter` (bad UUID / bad enum)
- `400 invalid_sort` (unknown sort property)
- `401 unauthenticated` (credentials required)
- `403 forbidden` (@PreAuthorize denied)
- `404 not_found` (`ResourceNotFoundException` or unmapped route)
- `409 conflict` (duplicate slug / sku, or DB unique-constraint race)
- `500 internal_error` (unhandled — logged server-side, no stack trace leaked)

## API versioning

Every route is served under `/api/v1` via `server.servlet.context-path`. A future v2 rev is a matter of duplicating
the controllers into a versioned package and switching the context-path — no per-controller path prefix required.

## Health endpoints

- `GET /api/v1/health` — friendly JSON status for the frontend
- `GET /api/v1/actuator/health` — Spring Boot Actuator probe (K8s liveness/readiness)

## Error format

Every non-2xx response returns a uniform body:

```json
{
  "status": 404,
  "error": "not_found",
  "message": "Product not found: onyx-two-piece-suit",
  "path": "/api/v1/products/onyx-two-piece-suit",
  "timestamp": "2026-07-15T14:22:03.821Z"
}
```

Validation failures include field-level `details`:

```json
{
  "status": 400,
  "error": "validation_failed",
  "message": "One or more fields failed validation.",
  "path": "/api/v1/orders",
  "timestamp": "…",
  "details": [
    { "field": "address.email", "message": "must not be blank" }
  ]
}
```
