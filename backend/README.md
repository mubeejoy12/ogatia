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
| `dev` (default) | H2 in-memory | `http://localhost:3000` | Local dev without Postgres install |
| `prod` | Postgres via env vars | Set via `ALLOWED_ORIGINS` | Production deployment |

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
