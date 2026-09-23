# Spring REST Boilerplate

A production-ready Spring Boot REST API boilerplate built with **Java 25**, **Spring Boot 4.1**, and **PostgreSQL**. Clone this repository to get a clean, scalable foundation for any new REST API project — authentication, database migrations, code quality enforcement, AOP logging, and a full testing infrastructure are all pre-wired so you can focus on building features from day one.

> **Virtual Threads PoC:** a learning proof of concept comparing platform and virtual threads (benchmarks + `/v1/virtual-threads/**` endpoints) lives in [`docs/virtual-threads-poc.md`](docs/virtual-threads-poc.md).

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Using This Boilerplate](#using-this-boilerplate)
5. [Project Structure](#project-structure)
6. [Architecture & Design](#architecture-design)
7. [Configuration & Environment Variables](#configuration-environment-variables)
8. [Database Setup & Migrations](#database-setup-migrations)
9. [Security](#security)
10. [API Reference](#api-reference)
11. [Build, Run & Deploy](#build-run-deploy)
12. [Testing Strategy](#testing-strategy)
13. [Code Quality & Tooling](#code-quality-tooling)
14. [Observability](#observability)
15. [Contribution Guidelines](#contribution-guidelines)
16. [Troubleshooting](#troubleshooting)

---

## Project Overview

This boilerplate eliminates repetitive setup work when starting a new Spring Boot project. It is opinionated but intentionally thin on business logic — the `user` and `role` modules are minimal reference examples that demonstrate the layered architecture and testing patterns. Delete what you don't need, keep the infrastructure that would take days to configure from scratch, and start shipping features immediately.

> **This is a starting point, not a framework.** There is no hidden abstraction layer. Every class is yours to read, modify, or remove.

**Key Features:**

- **REST API** — Spring MVC with versioned endpoints (`/v1/...`)
- **Authentication** — JWT-based stateless authentication (filter pre-wired, validation logic ready to implement); federated identity with `UserIdentity` entity supporting `LOCAL`, `AUTH0`, `GOOGLE`, `FACEBOOK`, and `GITHUB` providers
- **Database** — PostgreSQL with Liquibase schema migrations
- **Validation** — Jakarta Bean Validation with structured, field-level error responses
- **Auditing** — Automatic `created_by`, `updated_by`, and timestamp columns on every entity
- **Observability** — Spring Actuator + Micrometer + Prometheus metrics endpoint
- **AOP Logging** — Method-level execution logging across Controller, Service, and DAO layers
- **HTTP Client** — Pre-configured Apache `RestClient` bean for calling external APIs
- **Testing** — JUnit 5, Mockito, Testcontainers (real PostgreSQL), and test data builders
- **Code Quality** — Spotless (formatting), SpotBugs, PMD, JaCoCo coverage enforcement
- **CI/CD** — GitHub Actions for AI-assisted PR review and secret scanning

---

## Prerequisites

Ensure the following tools are installed before getting started:

**Required**

- **Java JDK 25+**
  Download from [Eclipse Temurin](https://adoptium.net/) or your preferred distribution.
  Verify: `java -version`

- **Maven 3.9+**
  Download from [maven.apache.org](https://maven.apache.org/download.cgi).
  Verify: `mvn -v`

- **Docker & Docker Compose v2+**
  Included with [Docker Desktop](https://www.docker.com/products/docker-desktop/).
  Required for the recommended Docker Compose workflow.
  Verify: `docker -v` and `docker compose version`

- **Git 2.x+**
  Required for version control and pre-commit hooks.
  Verify: `git --version`

**Optional (local PostgreSQL only)**

- **PostgreSQL 13+**
  Only needed if running the app without Docker.
  Download from [postgresql.org](https://www.postgresql.org/download/).

**For contributors**

- **pre-commit**
  Install once after cloning: `pip install pre-commit`
  Enforces code quality checks automatically on every commit.

---

## Quick Start

### Option A — Docker Compose (Recommended)

The fastest way to get the full stack running locally.

```bash
# 1. Clone the repository
git clone <repository_url>
cd spring-rest-boilerplate

# 2. Create your local environment file
cp .env.example .env
# Edit .env with your preferred values (see Configuration section)

# 3. Build the JAR first (Docker copies it from target/)
mvn clean package -DskipTests

# 4. Start PostgreSQL + application
docker compose up --build

# 5. Verify the application is running
curl http://localhost:8001/actuator/health
```

### Option B — Run Directly (Local PostgreSQL)

```bash
# 1. Clone and navigate
git clone <repository_url>
cd spring-rest-boilerplate

# 2. Ensure PostgreSQL is running and create the database
createdb mb-spring-boilerplate

# 3. Run with the local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local

# OR build and run the JAR
mvn clean package -DskipTests
java -Dspring.profiles.active=local -jar target/spring-rest-boilerplate-0.0.1.jar
```

Once started, the API is available at: `http://localhost:8001`

---

## Using This Boilerplate

Follow these steps when starting a new project from this template.

### 1. Rename the Base Package

The default package is `com.mb`. Replace it with your organisation/project namespace throughout the codebase.

```bash
# Example: rename com.mb → com.acme.payments
find src -type f -name "*.java" \
  | xargs sed -i '' 's/com\.mb/com.acme.payments/g'

# Rename the directory tree to match
mv src/main/java/com/mb src/main/java/com/acme/payments
mv src/test/java/com/mb src/test/java/com/acme/payments
```

### 2. Update Application Name & Server Port

Edit `src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: your-service-name   # used in logs and actuator /info

server:
  port: 8080   # change to your preferred port
```

Update `APP_PORT` in `.env` and the exposed port in `docker-compose.yml` accordingly.

### 3. Update the Maven Artifact

In `pom.xml`, update the `<groupId>`, `<artifactId>`, and `<name>` to match your project:

```xml
<groupId>com.acme</groupId>
<artifactId>payments-service</artifactId>
<name>payments-service</name>
```

### 4. Replace the Sample Modules (Optional)

The `modules/user` and `modules/auth/role` packages are reference implementations. Once you understand the layering pattern, you can:

- Delete them and start fresh with your own domain modules
- Or keep them as working examples alongside your new modules

### 5. Implement JWT Validation and Auth Flow

The `UserIdentity` entity and DAO layer are in place under `modules/auth/identity/`. The `AuthProvider` enum covers `LOCAL`, `AUTH0`, `GOOGLE`, `FACEBOOK`, and `GITHUB`. To complete the authentication flow:

1. **Verify the provider token** — fill in `JwtAuthenticationFilter` to validate the incoming JWT (use the provider's SDK or Nimbus JOSE+JWT; the `TODO` placeholder is there).
2. **Implement `UserIdentityService`** — look up the identity by `(provider, providerUserId)`, auto-link by email if a matching `User` already exists, or create a new `User` + `UserIdentity`.
3. **Issue your own JWT** — after a successful login, generate a short-lived JWT with `sub = user.uuid` and return it. All subsequent requests use this token; the provider is no longer involved.

### 6. Set CORS Origins

Update the `cors.allowed-origins` list in `application-local.yml`, `application-dev.yml`, and `application-prod.yml` to match your actual frontend URLs.

### 7. Install Pre-Commit Hooks

Every developer on the project should run this once after cloning:

```bash
pip install pre-commit
pre-commit install
pre-commit install --hook-type pre-push
```

---

## Project Structure

```
spring-rest-boilerplate/
├── .github/
│   └── workflows/
│       ├── claude-review.yml        # AI-assisted PR code review
│       └── gitleaks.yml             # Secret scanning on push
├── src/
│   ├── main/
│   │   ├── java/com/mb/
│   │   │   ├── MbSpringRestApplication.java     # Application entry point
│   │   │   │
│   │   │   ├── common/                          # Shared utilities & cross-cutting concerns
│   │   │   │   ├── annotation/                  # [future] Custom annotations (e.g. @AuditLog, @RateLimit)
│   │   │   │   ├── constant/                    # API routes, error/response message constants
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/                 # [future] Shared inbound request DTOs (pagination, filters)
│   │   │   │   │   └── response/                # Generic API response envelope DTOs
│   │   │   │   ├── enums/                       # Shared enumerations (EntityStatus)
│   │   │   │   ├── exception/                   # AppException, ErrorCode, GlobalExceptionHandler
│   │   │   │   ├── mapper/                      # [future] Shared mapping utilities / MapStruct config
│   │   │   │   └── util/                        # ApiResponseBuilder and other helpers
│   │   │   │
│   │   │   ├── infrastructure/                  # Technical infrastructure (no business logic)
│   │   │   │   ├── aop/logging/                 # AOP method-level execution logging
│   │   │   │   ├── cache/                       # [future] Cache config (Redis, Caffeine, etc.)
│   │   │   │   ├── event/                       # [future] Application events & listeners
│   │   │   │   ├── http/                        # Configured RestClient for external API calls
│   │   │   │   ├── messaging/                   # [future] Message broker integration (Kafka, RabbitMQ)
│   │   │   │   ├── persistence/                 # JPA config, auditing, BaseEntity
│   │   │   │   ├── scheduler/                   # [future] Scheduled tasks (@Scheduled)
│   │   │   │   ├── security/                    # JWT filter, SecurityConfig, entry point
│   │   │   │   └── web/
│   │   │   │       ├── config/                  # CorsConfig
│   │   │   │       └── filter/                  # RequestLoggingFilter — MDC request-ID correlation
│   │   │   │
│   │   │   └── modules/                         # Feature modules — one package per domain
│   │   │       ├── auth/
│   │   │       │   ├── identity/                # UserIdentity entity, DAO, repository, enums/AuthProvider (federated identity)
│   │   │       │   └── role/                    # Role entity, DAO, repository (reference)
│   │   │       └── user/                        # User entity, DTOs, DAO, service, controller (reference)
│   │   │
│   │   └── resources/
│   │       ├── application.yml                  # Base (shared) configuration
│   │       ├── application-local.yml            # Local development profile
│   │       ├── application-dev.yml              # Development environment profile
│   │       ├── application-qa.yml               # QA environment profile
│   │       ├── application-stage.yml            # Staging environment profile
│   │       ├── application-prod.yml             # Production environment profile
│   │       └── db/changelog/                    # Liquibase migration scripts
│   │           ├── db.changelog-master.yaml     # Master changelog (ordered includes)
│   │           ├── changes/                     # DDL schema migrations
│   │           │   ├── 001-create-roles-table.sql
│   │           │   ├── 002-create-users-table.sql
│   │           │   ├── 003-create-user-roles-table.sql
│   │           │   └── 005-create-user-identities-table.sql
│   │           ├── seeds/                       # DML seed data
│   │           │   └── 001-insert-roles.sql
│   │           └── environment-data/            # Context-gated, per-profile data
│   │               └── local/
│   │                   └── 001-local-sample-data.sql
│   │
│   └── test/
│       └── java/com/mb/
│           ├── base/                            # AbstractBaseIntegrationTest, TestJpaConfig
│           ├── common/                          # Tests: exception handler, response builder
│           ├── infrastructure/                  # Tests: security, logging, HTTP client, auditing
│           └── modules/                         # Unit + integration tests mirroring src/modules
│               ├── auth/
│               │   ├── identity/
│               │   └── role/
│               └── user/
│
├── .env.example                                 # Environment variable template — copy to .env
├── .pre-commit-config.yaml                      # Pre-commit hook configuration
├── docker-compose.yml                           # Local Docker services (PostgreSQL + app)
├── Dockerfile.local                             # Application Docker image for local use
└── pom.xml                                      # Maven build, dependencies, quality plugins
```

> Packages marked `[future]` are intentional placeholders. They indicate where to add that concern when your project needs it — keeping the structure consistent across teams and projects.

### Module Layout Convention

Each feature module follows this internal structure. Packages marked `[future]` are added only when the module needs them — do not create empty packages upfront.

```
modules/<feature>/
├── controller/              # REST controller — request/response mapping only
├── service/                 # Business logic — interface + implementation
├── dao/                     # Data access — interface + implementation (wraps repository)
├── repository/              # Spring Data JPA interface
├── entity/                  # JPA entity classes
├── dto/
│   ├── request/             # Inbound DTOs — validated @RequestBody / @RequestParam payloads
│   └── response/            # Outbound DTOs — never expose entities directly to the API
├── mapper/                  # [future] Entity ↔ DTO mapping (MapStruct or manual)
├── validator/               # [future] Custom @Constraint validators for this module
├── event/                   # [future] Domain events published/consumed within the module
└── scheduler/               # [future] @Scheduled tasks scoped to this module
```

**Rule of thumb:** a module owns everything for one domain concept. Cross-module dependencies should flow through service interfaces, never directly between DAOs or repositories.

---

## Architecture & Design

### Layered Architecture

```
HTTP Request
     │
     ▼
┌─────────────────┐
│   Controller    │  Validates input, maps to/from DTOs, enforces authentication
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│    Service      │  Business logic, orchestration, transaction boundaries
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│      DAO        │  Data access abstraction, exception wrapping
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Repository     │  Spring Data JPA interface (query execution)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   PostgreSQL    │  Persistent storage
└─────────────────┘
```

### Request Lifecycle

```
Client
  │
  ├─► JwtAuthenticationFilter   ─── Validates Bearer token, sets SecurityContext
  │
  ├─► SecurityConfig             ─── Authorizes request against security rules
  │
  ├─► Controller                 ─── Deserializes body, validates @Valid constraints
  │
  ├─► Service                    ─── Executes business logic
  │
  ├─► DAO → Repository           ─── Queries database via JPA
  │
  └─► GlobalExceptionHandler      ─── Catches all exceptions, returns structured JSON
```

### Standard API Response Envelope

Every response — success or error — is wrapped in a consistent structure:

```json
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "timestamp": "2024-01-15T10:30:00Z",
  "errors": null,
  "errorCode": null
}
```

**Error response example (validation failure):**

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "timestamp": "2024-01-15T10:30:00Z",
  "errors": [
    {
      "field": "email",
      "message": "must not be blank",
      "rejectedValue": ""
    }
  ],
  "errorCode": "VALIDATION_ERROR"
}
```

### Key Design Patterns

- **DAO Pattern** — Decouples business logic from persistence; enables testing with mocks
- **Builder Pattern** — `ApiResponse`, `HttpRequest` built via fluent builders
- **AOP Logging** — Cross-cutting logging concern via `@Aspect`; no logging boilerplate in business code
- **Generic DAO** — Type-safe data access with `Class<T>` projection support

---

## Configuration & Environment Variables

### Environment File Setup

The application uses a `.env` file for local configuration. A template is provided at `.env.example`.

```bash
# Copy the template and fill in your values
cp .env.example .env
```

> Never commit `.env` to version control. It is already listed in `.gitignore`.

#### Available Variables

| Variable | Description | Default / Example |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` |
| `APP_PORT` | Host port mapped to container port 8001 | `8001` |
| `DATABASE_NAME` | PostgreSQL database name | `mb-spring-boilerplate` |
| `DATABASE_HOST` | PostgreSQL host | `postgres` (Docker) / `localhost` |
| `DATABASE_PORT` | PostgreSQL port | `5432` |
| `DATABASE_USERNAME` | Database username | `postgres` |
| `DATABASE_PASSWORD` | Database password | `changeme_local_only` |

### Spring Profiles

Select the appropriate profile by setting `SPRING_PROFILES_ACTIVE` in your `.env` file or as a JVM argument.

| Profile | When to Use | SQL Logging | CORS Origins |
| :--- | :--- | :---: | :--- |
| `local` | Local development — uses hardcoded defaults | ✓ Enabled | `localhost:3000`, `localhost:4200` |
| `dev` | Shared development server | ✗ Disabled | `dev.dummy-frontend-url.com` |
| `qa` | Shared QA server | ✗ Disabled | `qa.dummy-frontend-url.com` |
| `stage` | Staging environment | ✗ Disabled | `stage.dummy-frontend-url.com` |
| `prod` | Production deployment | ✗ Disabled | `prod.dummy-frontend-url.com` |
| `test` | Automated tests — managed by Testcontainers | ✓ Enabled | `localhost:3000` |

> For `dev`, `qa`, `stage`, and `prod`, all datasource credentials are injected via environment variables — no hardcoded values.

---

## Database Setup & Migrations

### Liquibase Overview

Schema versioning is managed by [Liquibase](https://www.liquibase.org/). Migrations run automatically at application startup. Manual intervention is not required.

**Master changelog:** `src/main/resources/db/changelog/db.changelog-master.yaml`

Migrations run in three ordered groups, each in its own folder: schema (DDL) → seed data (DML) → environment-specific data (context-gated).

### Schema Migrations (`changes/`)

Run in every environment. Each changeset includes a rollback block.

| # | File | Description |
| :---: | :--- | :--- |
| 001 | `001-create-roles-table.sql` | Creates `roles` table with UUID, name, description, audit columns, and indexes |
| 002 | `002-create-users-table.sql` | Creates `users` table with UUID, name, email, audit columns; unique indexes on `uuid` and `email` |
| 003 | `003-create-user-roles-table.sql` | Creates `user_roles` junction table with FK constraints, unique constraint on `uuid`, and indexes on `user_id` / `role_id` |
| 005 | `005-create-user-identities-table.sql` | Creates `user_identities` table — federated identity per provider (`LOCAL`, `AUTH0`, `GOOGLE`, `FACEBOOK`, `GITHUB`); index on `user_id` |

### Seed Data (`seeds/`)

| # | File | Description |
| :---: | :--- | :--- |
| 001 | `001-insert-roles.sql` | Seeds `ADMIN` and `USER` roles; uses `ON CONFLICT DO NOTHING` (idempotent) |

### Environment-Specific Data (`environment-data/`)

Data scoped to a single environment, gated by Liquibase `context:<profile>` on the changeset so it only applies when `spring.liquibase.contexts` matches that profile. Every profile (`local`, `dev`, `qa`, `stage`, `prod`) sets `spring.liquibase.contexts` in its `application-<profile>.yml`.

| Profile | File | Description |
| :--- | :--- | :--- |
| `local` | `local/001-local-sample-data.sql` | Inserts a sample local dev user (`local.dev@example.com`) and a matching `LOCAL` identity; context `local` |

### Adding a New Migration

**Schema change** (DDL — runs everywhere):
1. Create `src/main/resources/db/changelog/changes/<NNN>-<description>.sql`
2. Include a `--rollback` block
3. Add the file reference under the schema section of `db.changelog-master.yaml`

```sql
--changeset author:your-name
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
--rollback ALTER TABLE users DROP COLUMN phone;
```

**Seed data** (DML):
1. Create `src/main/resources/db/changelog/seeds/<NNN>-<description>.sql`
2. Use `ON CONFLICT DO NOTHING` for idempotency and include a `--rollback` block
3. Add the file reference under the seed section of `db.changelog-master.yaml`

```sql
--changeset author:your-name
INSERT INTO lookup_values (code, label) VALUES ('FOO', 'Foo label')
ON CONFLICT (code) DO NOTHING;
--rollback DELETE FROM lookup_values WHERE code = 'FOO';
```

---

## Security

### Authorization Rules

| Path | Access Level | Notes |
| :--- | :--- | :--- |
| `OPTIONS /**` | Public | CORS preflight — no auth required |
| `GET /actuator/health` | Public | Health probe endpoint |
| `GET /actuator/info` | Public | Build/version info |
| `GET /actuator/**` | `ADMIN` role only | Metrics, Prometheus, env details |
| All other paths | Authenticated | Valid JWT required |

### Security Configuration Highlights

- **Stateless sessions** — No `HttpSession`; each request must carry a valid JWT
- **CSRF disabled** — Not needed for token-based REST APIs
- **Form login disabled** — Pure REST; no HTML login forms
- **Custom entry point** — Returns `401 UNAUTHORIZED` as JSON (not a redirect)

### Allowed CORS Headers

`Authorization`, `Content-Type`, `X-Correlation-Id`, `X-Tenant-Id`, `X-Request-Id`, `X-Api-Version`, `X-Idempotency-Key`

---

## API Reference

**Base URL:** `http://localhost:8001`

### Health Check

```
GET /actuator/health
```

Returns `{ "status": "UP" }` — no authentication required.

### Metrics

```
GET /actuator/metrics
GET /actuator/prometheus
```

Requires `ADMIN` role.

---

## Build, Run & Deploy

### Build

```bash
# Full build with all quality checks
mvn clean verify

# Skip tests (faster, use sparingly)
mvn clean package -DskipTests

# Skip quality checks (e.g., spotbugs, pmd) — NOT recommended for CI
mvn clean package -DskipTests -Dspotbugs.skip=true -Dpmd.skip=true
```

### Run Locally

```bash
# Using Maven Spring Boot plugin
mvn spring-boot:run -Dspring-boot.run.profiles=local

# Using built JAR
java -Dspring.profiles.active=local \
     -jar target/spring-rest-boilerplate-0.0.1.jar
```

### Docker Compose (Local Full Stack)

```bash
# Start everything (builds image if needed)
docker compose up --build

# Start in background
docker compose up -d --build

# View logs
docker compose logs -f app

# Stop and remove containers
docker compose down

# Stop and remove containers + volumes (wipes DB data)
docker compose down -v
```

---

## Testing Strategy

### Test Types

| Type | Annotation | Scope | Speed |
| :--- | :--- | :--- | :---: |
| Unit — Controller | `@WebMvcTest` + `@AutoConfigureRestTestClient` | Web slice only; service mocked via `@MockitoBean`; security via `MockMvcSecurityConfig` | Fast |
| Unit — Service / DAO | `@ExtendWith(MockitoExtension)` | Single class; all dependencies mocked | Fast |
| Integration | `@SpringBootTest` + `@AutoConfigureMockMvc` + `@AutoConfigureRestTestClient` | Full Spring context; real PostgreSQL via Testcontainers `@ServiceConnection`; security via `MockMvcSecurityConfig` | Slow |
| Repository | `@DataJpaTest` | JPA slice only; real PostgreSQL via Testcontainers `@ServiceConnection` | Medium |

### Test Structure

```
src/test/java/com/mb/
├── base/
│   ├── AbstractBaseIntegrationTest.java    # Full-stack integration test base (@SpringBootTest + @ServiceConnection)
│   ├── AbstractBaseJpaTest.java            # JPA slice test base (@DataJpaTest + @ServiceConnection)
│   ├── MockMvcSecurityConfig.java          # MockMvcBuilderCustomizer that calls springSecurity() — used by @WebMvcTest and AbstractBaseIntegrationTest
│   ├── TestContainersConfig.java           # @ServiceConnection PostgreSQL container bean (shared)
│   └── TestJpaConfig.java                  # Test-specific JPA auditing configuration
├── common/
│   ├── exception/
│   │   ├── AppExceptionTest.java           # Custom exception construction
│   │   └── GlobalExceptionHandlerTest.java # All exception handler paths
│   └── util/
│       └── ApiResponseBuilderTest.java     # Response builder edge cases
├── infrastructure/
│   ├── aop/logging/
│   │   └── LoggerAspectTest.java           # AOP advice execution
│   ├── http/
│   │   ├── HttpClientTest.java             # External HTTP client behavior
│   │   └── HttpRequestTest.java            # Request DTO builder
│   ├── persistence/config/
│   │   └── SecurityAuditorAwareTest.java   # Auditor resolution (authenticated vs. system)
│   └── security/
│       ├── config/
│       │   └── CustomAuthenticationEntryPointTest.java
│       ├── filter/
│       │   └── JwtAuthenticationFilterTest.java
│       └── integration/
│           └── SecurityIntegrationTest.java     # 401 / 403 / 200 full-stack access-control tests
└── modules/
    ├── auth/
    │   ├── identity/
    │   │   ├── testdata/
    │   │   │   └── UserIdentityTestDataBuilder.java  # UserIdentity fixture factory
    │   │   └── unit/dao/
    │   │       └── UserIdentityDaoImplTest.java
    │   └── role/
    │       ├── testdata/
    │       │   └── RoleTestDataBuilder.java    # Role fixture factory
    │       └── unit/dao/
    │           └── RoleDaoImplTest.java
    └── user/
        ├── testdata/
        │   └── UserTestDataBuilder.java    # User fixture factory
        ├── unit/
        │   ├── controller/
        │   │   └── UserControllerTest.java         # @WebMvcTest + RestTestClient
        │   ├── service/
        │   │   └── UserServiceImplTest.java
        │   └── dao/
        │       └── UserDaoImplTest.java
        └── integration/
            ├── controller/
            │   └── UserControllerIntegrationTest.java  # Full-stack (happy path, 404, 400, 401)
            └── repository/
                └── UserRepositoryIntegrationTest.java
```

### Security Testing in `@WebMvcTest`

Spring Boot 4.x does not automatically apply the Spring Security filter chain to MockMvc. Without explicit wiring, `anyRequest().authenticated()` is never enforced and unauthenticated requests reach the controller. Import `MockMvcSecurityConfig`, `SecurityConfig`, and `CustomAuthenticationEntryPoint` in every `@WebMvcTest` that verifies security behaviour:

```java
@WebMvcTest(controllers = MyController.class)
@AutoConfigureRestTestClient
@Import({ ApiResponseBuilder.class, MockMvcSecurityConfig.class,
          SecurityConfig.class, CustomAuthenticationEntryPoint.class })
class MyControllerTest {
    // @WithMockUser per authenticated test; omit for the 401 test
}
```

`MockMvcSecurityConfig` registers a `MockMvcBuilderCustomizer` that calls `apply(springSecurity())`, routing MockMvc (and `RestTestClient`) through the real `FilterChainProxy`. `SecurityConfig` supplies the `springSecurityFilterChain` bean it requires. `CustomAuthenticationEntryPoint` must be explicitly imported because `@WebMvcTest` does not component-scan `@Component` beans outside the web slice.

### Security Testing in `@SpringBootTest` (Integration Tests)

`AbstractBaseIntegrationTest` handles everything — no extra setup needed in individual integration test classes. It carries both `@AutoConfigureMockMvc` and `@Import(MockMvcSecurityConfig.class)`:

- `@AutoConfigureMockMvc` activates `MockMvcAutoConfiguration`, creating a `MockMvc` bean that applies all `MockMvcBuilderCustomizer` beans in the context.
- `MockMvcSecurityConfig` contributes the customizer that calls `apply(springSecurity())`.
- `RestTestClient` (from `@AutoConfigureRestTestClient`) then binds to that `MockMvc` bean and routes all requests through Spring Security.

`SecurityConfig` and `CustomAuthenticationEntryPoint` are **not** needed in integration test `@Import`s — the full `@SpringBootTest` context discovers them via component scanning automatically.

### Running Tests

```bash
# Run all tests
mvn test

# Run only unit tests (by convention — no Testcontainers)
mvn test -Dtest="**/*Test" -Dexclude="**/*IntegrationTest"

# Run integration tests only
mvn test -Dtest="**/*IntegrationTest"

# Run with coverage report
mvn verify
# Report generated at: target/site/jacoco/index.html
```

### Test Data Builders

Use test data builders to create consistent, readable test fixtures:

```java
// User fixtures
User user     = UserTestDataBuilder.buildUser();                          // fixed UUID — unit tests
User user     = UserTestDataBuilder.buildUser("jane@example.com");        // random UUID — integration tests
User minimal  = UserTestDataBuilder.buildUserWithNullNames("j@x.com");    // null names — edge-case tests

// UserIdentity fixtures
UserIdentity local    = UserIdentityTestDataBuilder.buildLocalIdentity(user);         // LOCAL, ACTIVE
UserIdentity auth0    = UserIdentityTestDataBuilder.buildAuth0Identity(user);         // AUTH0, ACTIVE
UserIdentity any      = UserIdentityTestDataBuilder.buildIdentity(user, AuthProvider.GOOGLE, "google|xyz"); // arbitrary provider
UserIdentity inactive = UserIdentityTestDataBuilder.buildInactiveLocalIdentity(user); // LOCAL, INACTIVE

// Role fixtures
Role admin     = RoleTestDataBuilder.buildAdminRole();
Role user      = RoleTestDataBuilder.buildUserRole();
Role moderator = RoleTestDataBuilder.buildModeratorRole();
```

### Coverage Enforcement

JaCoCo is configured to fail the build if coverage drops below the defined thresholds. The following packages are excluded from coverage requirements:

- DTOs and constants (no logic)
- Main application bootstrap class
- Configuration classes

---

## Code Quality & Tooling

### Pre-Commit Hooks

Install hooks once after cloning:

```bash
pip install pre-commit
pre-commit install
pre-commit install --hook-type pre-push
```

The following checks run automatically on every `git commit` or `git push`:

| Hook | Trigger | Description |
| :--- | :---: | :--- |
| `spotless-apply` | commit | Auto-formats code with Google Java Format before committing |
| `spotbugs` | commit | Static analysis — fails on Medium or higher severity bugs |
| `pmd` | commit | Code quality rules — detects common anti-patterns |
| `unit-tests` | commit | Runs the full test suite |
| `coverage-check` | commit | Enforces JaCoCo minimum coverage thresholds |
| `commitizen-check` | commit-msg | Validates commit message against Conventional Commits format |
| `gitleaks` | commit + commit-msg | Scans staged changes for hardcoded secrets or credentials |

### Commit Message Format

This project uses [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>

Types: feat, fix, docs, style, refactor, test, chore, perf
```

**Examples:**

```
feat(user): add GET endpoint to retrieve user by UUID
fix(auth): correct JWT expiry validation logic
test(user): add integration test for user lookup
```

### Manual Quality Commands

```bash
# Format code (auto-fix)
mvn spotless:apply

# Check formatting without fixing
mvn spotless:check

# Run SpotBugs analysis
mvn spotbugs:check

# Run PMD analysis
mvn pmd:check

# Generate JaCoCo coverage report
mvn jacoco:report
```

### CI/CD — GitHub Actions

Two workflows are pre-configured under `.github/workflows/`:

- **`claude-review.yml`** — Triggers on PR open or synchronize. Runs an AI-assisted code review that checks architecture, security, test coverage, and API design. Posts structured feedback as a PR comment.

- **`gitleaks.yml`** — Triggers on push to `main`, `development`, and `staging`. Scans the full git history for leaked secrets or hardcoded credentials.

---

## Observability

### Actuator Endpoints

Spring Actuator endpoints are exposed at `/actuator`. Public endpoints require no authentication; others require the `ADMIN` role.

| Endpoint | Access | Description |
| :--- | :---: | :--- |
| `GET /actuator/health` | Public | Application health status and dependency checks |
| `GET /actuator/info` | Public | Build metadata and application version |
| `GET /actuator/metrics` | ADMIN | JVM memory, HTTP request counts, datasource stats |
| `GET /actuator/prometheus` | ADMIN | Prometheus-compatible scrape endpoint for metrics collection |

### AOP Logging

`LoggerAspect` automatically logs method entry, exit, and execution time across all layers:

```
[CONTROLLER] → UserController.userByUuid() | args: [550e8400-...]
[SERVICE]    → UserServiceImpl.findByUuid() | args: [550e8400-...]
[DAO]        → UserDaoImpl.findByUuid()     | args: [550e8400-...]
[DAO]        ← UserDaoImpl.findByUuid()     | duration: 12ms
[SERVICE]    ← UserServiceImpl.findByUuid() | duration: 15ms
[CONTROLLER] ← UserController.userByUuid() | duration: 18ms
```

Exceptions are logged with full context at the appropriate layer.

---

## Contribution Guidelines

### Getting Started

1. Fork the repository and create a feature branch from `development`:
   ```bash
   git checkout -b feat/your-feature-name development
   ```

2. Install pre-commit hooks (see [Code Quality](#code-quality-tooling))

3. Make changes following the conventions below

4. Push and open a Pull Request targeting `development`

### Coding Conventions

- **Package structure:** Place new feature code under `src/main/java/com/mb/modules/<feature>/`
- **Layer separation:** Never skip layers — controllers call services, services call DAOs
- **No entity leakage:** Never return JPA entities directly from controllers; always map to DTOs
- **Exception handling:** Throw `AppException` with an appropriate `HttpStatus`; the global handler does the rest
- **Constants:** Add new API paths to `ApiEndpoint`, messages to `ExceptionMessage` or `ResponseMessage`
- **Auditing:** Extend `BaseEntity` for all new entities to get audit columns automatically

### Adding a New Feature Module

1. Create the core packages under `modules/<feature>/`: `entity/`, `dto/request/`, `dto/response/`, `repository/`, `dao/`, `service/`, `controller/`
2. Add only the optional packages (`mapper/`, `validator/`, `event/`, `scheduler/`) when your module actually needs them
3. Add Liquibase migration(s) for any new tables under `db/changelog/changes/` and reference them in the master changelog
4. Implement the layers in order: Entity → Repository → DAO → Service → Controller
5. Create a `testdata/<Feature>TestDataBuilder.java` in the test tree before writing any test
6. Write unit tests for Service and DAO layers (mock all dependencies)
7. Write at least one integration test for the Controller layer using `AbstractBaseIntegrationTest`

### Pull Request Checklist

- [ ] All tests pass (`mvn verify`)
- [ ] Code is formatted (`mvn spotless:check`)
- [ ] No SpotBugs violations (`mvn spotbugs:check`)
- [ ] Liquibase migration included for schema changes (with rollback)
- [ ] New endpoints documented in this README
- [ ] Commit messages follow Conventional Commits format

---

## Troubleshooting

### `java.lang.UnsupportedClassVersionError`

You're running the JAR with a JRE older than Java 25.

```bash
java -version   # Must show 25 or higher
```

### Pre-commit hook fails: formatting error

`spotless-apply` runs automatically on commit and reformats code in-place. If the hook exits non-zero, re-stage the reformatted files and retry:

```bash
git add -u
git commit
```

To reformat manually at any time:

```bash
mvn spotless:apply
```

### Port `8001` already in use

```bash
# Find the process using the port
lsof -i :8001

# Kill it, or change APP_PORT in your .env
APP_PORT=8002
```

### Tests fail with `ContainerLaunchException`

Testcontainers requires Docker to be running.

```bash
# Verify Docker is running
docker info

# Pull the PostgreSQL image manually if needed
docker pull postgres:18-alpine
```

---
