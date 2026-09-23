# CLAUDE.md — Spring REST Boilerplate

A guide for AI assistants working on this codebase. Read this before making any changes.

---

## 1. Project Overview

**spring-rest-boilerplate** is a production-ready Spring Boot REST API foundation built to eliminate repetitive project setup. It is opinionated on infrastructure (security, database, code quality, observability, testing) and intentionally thin on business logic — the `user` and `role` modules are minimal reference implementations only.

- **Core purpose:** Give teams a clean, pre-wired starting point for any new REST API so they can focus on features from day one.
- **Target users:** Backend Java developers starting a new Spring Boot microservice or API project.
- **Not a framework:** Every class is plain Java/Spring, readable and modifiable. There are no hidden abstractions.

---

## 2. Tech Stack

| Category | Technology | Version |
| :--- | :--- | :--- |
| Language | Java | 25 |
| Framework | Spring Boot | 4.1.0 |
| Web | Spring MVC (spring-boot-starter-web) | — |
| Security | Spring Security + JWT filter | — |
| Persistence | Spring Data JPA + Hibernate | — |
| Database | PostgreSQL | 13+ (runtime), 18-alpine (Docker) |
| Migrations | Liquibase | — |
| HTTP Client | Apache HttpClient5 (`RestClient`) | — |
| Observability | Spring Actuator + Micrometer + Prometheus | — |
| Boilerplate reduction | Lombok | — |
| Testing | JUnit 5, Mockito, Testcontainers | — |
| Code formatting | Spotless (Google Java Format) | 3.6.0 |
| Static analysis | SpotBugs | 4.10.2.0 |
| Code quality | PMD | 3.28.0 |
| Coverage | JaCoCo | 0.8.15 |
| Build | Maven | 3.9+ |
| Containers | Docker + Docker Compose v2 | — |
| CI/CD | GitHub Actions | — |
| Pre-commit | pre-commit + commitizen | — |

---

## 3. Project Structure

```
spring-rest-boilerplate/
├── .github/workflows/
│   ├── claude-review.yml        # AI-assisted PR review on every PR
│   └── gitleaks.yml             # Secret scanning on push
├── src/
│   ├── main/java/com/mb/
│   │   ├── MbSpringRestApplication.java     # Entry point
│   │   ├── common/                          # Shared, cross-cutting utilities
│   │   │   ├── constant/                    # ApiEndpoint, AppConstant, ExceptionMessage, ResponseMessage
│   │   │   ├── dto/response/                # ApiResponse<T>, PagedResponse<T>, ErrorDetail
│   │   │   ├── enums/                       # EntityStatus
│   │   │   ├── exception/                   # AppException, ErrorCode, GlobalExceptionHandler
│   │   │   └── util/                        # ApiResponseBuilder
│   │   ├── infrastructure/                  # Technical plumbing — no business logic here
│   │   │   ├── aop/logging/                 # LoggerAspect — method-level execution logging
│   │   │   ├── http/                        # HttpClient, HttpClientConfig, HttpRequest
│   │   │   ├── persistence/                 # JpaConfig, SecurityAuditorAware, BaseEntity
│   │   │   ├── security/                    # SecurityConfig, JwtAuthenticationFilter, CustomAuthenticationEntryPoint
│   │   │   └── web/
│   │   │       ├── config/                  # CorsConfig
│   │   │       └── filter/                  # RequestLoggingFilter — MDC request-ID correlation
│   │   └── modules/                         # Feature modules — one package per domain
│   │       ├── auth/
│   │       │   ├── identity/                # UserIdentity entity, DAO, repository, enums/AuthProvider (federated identity)
│   │       │   └── role/                    # Role entity, DAO, repository (reference)
│   │       └── user/                        # User entity, DTOs, DAO, service, controller (reference)
│   ├── main/resources/
│   │   ├── application.yml                  # Base config (server port, actuator, http client)
│   │   ├── application-local.yml            # Local dev: datasource + CORS
│   │   ├── application-dev.yml              # Dev environment
│   │   ├── application-qa.yml               # QA environment
│   │   ├── application-stage.yml            # Staging environment
│   │   ├── application-prod.yml             # Production
│   │   └── db/changelog/                    # Liquibase migrations
│   └── test/java/com/mb/
│       ├── base/                            # AbstractBaseIntegrationTest, AbstractBaseJpaTest, MockMvcSecurityConfig, TestContainersConfig, TestJpaConfig
│       ├── common/                          # Tests for exception handler, response builder
│       ├── infrastructure/                  # Tests for security, logging, HTTP client, auditing
│       └── modules/                         # Unit + integration tests mirroring src/modules
├── .env.example                             # Template — copy to .env for local dev
├── .pre-commit-config.yaml                  # Pre-commit hooks
├── docker-compose.yml                       # Local full-stack (PostgreSQL + app)
├── Dockerfile.local                         # App image for local Docker use
└── pom.xml                                  # Maven build + quality plugins
```

### Module Layout Convention

Every feature module under `modules/<feature>/` follows this internal structure:

```
modules/<feature>/
├── controller/          # REST controller — request/response mapping only, no business logic
├── service/             # Business logic — interface + implementation
├── dao/                 # Data access abstraction — interface + implementation wrapping the repository
├── repository/          # Spring Data JPA interface
├── entity/              # JPA entity classes
├── dto/
│   ├── request/         # Inbound validated DTOs
│   └── response/        # Outbound DTOs — never expose entities directly
├── mapper/              # [future] MapStruct or manual entity↔DTO mapping
├── validator/           # [future] Custom @Constraint validators
├── event/               # [future] Domain events
└── scheduler/           # [future] @Scheduled tasks scoped to this module
```

> Packages marked `[future]` should only be created when the module actually needs them. Do not create empty packages upfront.

---

## 4. Setup Instructions

### Prerequisites

- Java JDK 25 — verify: `java -version`
- Maven 3.9+ — verify: `mvn -v`
- Docker & Docker Compose v2+ — verify: `docker compose version`
- Git 2.x+

For contributors: `pip install pre-commit`

### Option A — Docker Compose (Recommended)

```bash
# 1. Clone
git clone <repository_url>
cd spring-rest-boilerplate

# 2. Create local environment file
cp .env.example .env
# Edit .env — set DATABASE_NAME, DATABASE_PORT, DATABASE_PASSWORD at minimum

# 3. Build JAR (Docker copies from target/)
mvn clean package -DskipTests

# 4. Start full stack
docker compose up --build

# 5. Verify
curl http://localhost:8001/actuator/health
```

### Option B — Local PostgreSQL

```bash
# 1. Create the database
createdb mb-spring-boilerplate

# 2. Run with local profile
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Environment Variables

| Variable | Description | Example |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `local` |
| `APP_PORT` | Host port mapped to container | `8001` |
| `DATABASE_NAME` | PostgreSQL database name | `mb-spring-boilerplate` |
| `DATABASE_HOST` | PostgreSQL host | `postgres` (Docker) / `localhost` |
| `DATABASE_PORT` | PostgreSQL port | `5432` |
| `DATABASE_USERNAME` | DB username | `postgres` |
| `DATABASE_PASSWORD` | DB password | `changeme_local_only` |

> Never commit `.env`. It is already in `.gitignore`.

### Spring Profiles

| Profile | Use Case | SQL Logging |
| :--- | :--- | :---: |
| `local` | Local dev — hardcoded defaults | Enabled |
| `dev` | Shared dev server | Disabled |
| `qa` | Shared QA server | Disabled |
| `stage` | Staging environment | Disabled |
| `prod` | Production | Disabled |
| `test` | Automated tests (Testcontainers) | Enabled |

### Pre-commit Hooks (Contributors)

```bash
pip install pre-commit
pre-commit install
pre-commit install --hook-type pre-push
```

---

## 5. Development Guidelines

### Coding Standards

- **Formatter:** Google Java Format, enforced by Spotless. Auto-fix with `mvn spotless:apply` before committing.
- **Style:** Standard Java conventions — `PascalCase` for classes, `camelCase` for methods/fields, `UPPER_SNAKE_CASE` for constants.
- **Lombok:** Use `@Getter`, `@Setter`, `@RequiredArgsConstructor`, `@Builder` where appropriate. Avoid `@Data` on JPA entities.
- **No Javadoc on obvious code.** Only document non-trivial logic with inline comments.

### Naming Conventions

| Artifact | Pattern | Example |
| :--- | :--- | :--- |
| Entity | `<Domain>` | `User`, `Role` |
| Repository | `<Domain>Repository` | `UserRepository` |
| DAO interface | `<Domain>Dao` | `UserDao` |
| DAO implementation | `<Domain>DaoImpl` | `UserDaoImpl` |
| Service interface | `<Domain>Service` | `UserService` |
| Service implementation | `<Domain>ServiceImpl` | `UserServiceImpl` |
| Controller | `<Domain>Controller` | `UserController` |
| Request DTO | `<Action><Domain>RequestDto` | `CreateUserRequestDto` |
| Response DTO | `<Domain>ResponseDto` | `UserResponseDto` |
| Test data builder | `<Domain>TestDataBuilder` | `UserTestDataBuilder` |

### API Endpoint Conventions

- All endpoints are versioned: `/v1/...`
- All paths are defined as constants in `ApiEndpoint.java` — never use inline string literals in controllers.
- REST conventions: `GET` for reads, `POST` for creates, `PUT`/`PATCH` for updates, `DELETE` for deletes.
- Return correct HTTP status codes: `200 OK`, `201 Created`, `204 No Content`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `404 Not Found`.

### Exception Handling

- Throw `AppException` with an `HttpStatus` and optionally an `ErrorCode`. Do not throw raw `RuntimeException`.
- `GlobalExceptionHandler` catches all exceptions and returns the standard JSON envelope — do not write `try/catch` in controllers.
- Add new error messages to `ExceptionMessage.java` and new error codes to `ErrorCode.java`.
- In DAO `save` methods, catch `DataIntegrityViolationException` before the generic `Exception` catch and map it to `HttpStatus.CONFLICT` / `ErrorCode.DUPLICATE_RESOURCE`.
- `GlobalExceptionHandler` handles 405 (`HttpRequestMethodNotSupportedException`) and 415 (`HttpMediaTypeNotSupportedException`) in addition to the standard 400/401/404/500 paths.
- Available `ErrorCode` values: `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `RESOURCE_NOT_FOUND`, `CONFLICT`, `DUPLICATE_RESOURCE`, `INVALID_REQUEST`, `INTERNAL_SERVER_ERROR`.

### Standard API Response Envelope

Every response uses `ApiResponse<T>` via `ApiResponseBuilder`:

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

Always use `responseBuilder.success(...)` or `responseBuilder.error(...)` — never build raw `ResponseEntity` responses in controllers.

### Database & Entity Rules

- All entities must extend `BaseEntity` to inherit `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`.
- Schema changes (DDL) go in `db/changelog/changes/` — never use `ddl-auto: create` or `ddl-auto: update` in non-test profiles.
- Seed data (DML) goes in `db/changelog/seeds/` — runs in all environments.
- Every migration and seed file must include a `--rollback` block.
- Schema migrations are numbered sequentially: `006-<description>.sql`. Seeds have their own sequence: `002-<description>.sql`.
- Seed inserts must use `ON CONFLICT (...) DO NOTHING` to be idempotent (safe to re-run).
- Environment-specific, context-gated data goes in `db/changelog/environment-data/<profile>/<NNN>-<description>.sql` (e.g. `environment-data/local/001-local-sample-data.sql`). Each changeset carries a `context:<profile>` attribute (e.g. `context:local`) so it only applies when `spring.liquibase.contexts` matches — every profile (`local`, `dev`, `qa`, `stage`, `prod`) sets this property. Reference the file under its own section in `db.changelog-master.yaml`, after schema and seed includes.

### Layer Boundaries

```
Controller → Service → DAO → Repository → Database
```

- **Never skip a layer.** Controllers must not call repositories directly.
- **Never return JPA entities from controllers.** Map to DTOs first.
- **Business logic belongs in Service.** Controllers only handle HTTP concerns.
- **Cross-module dependencies** go through Service interfaces, never between DAOs or repositories.

---

## 6. AI Assistant Guidelines

### Behavior Rules

- **Read before modifying.** Always read the file or relevant files before suggesting or making changes. Do not assume structure from filenames alone.
- **Follow existing patterns.** If a pattern exists in the codebase (e.g., how a controller returns responses, how a DAO is structured), replicate it exactly for new code.
- **Minimal scope.** Only change what is asked. Do not refactor surrounding code, add comments, rename variables, or "improve" adjacent code unless explicitly requested.
- **No speculative additions.** Do not add error handling, fallbacks, logging, validation, or configuration for scenarios not described in the task. The existing infrastructure handles most cross-cutting concerns already.
- **No new files unless necessary.** Prefer editing existing files. Do not create utility classes, helpers, or abstractions for one-time operations.

### When Modifying Code

- Enforce Google Java Format — run `mvn spotless:apply` mentally: don't produce code that would fail `spotless:check`.
- All new API paths must be added to `ApiEndpoint.java`.
- All new error/response messages must be added to `ExceptionMessage.java` or `ResponseMessage.java`.
- All new entities must extend `BaseEntity`.
- All new Liquibase migrations must be referenced in `db.changelog-master.yaml`.

### Handling Ambiguity

- If the task is ambiguous about which layer to modify (e.g., "add user lookup"), default to the full stack: entity change → migration → DAO → service → controller.
- If the task says "add an endpoint", also ask about or infer the corresponding service and DAO methods needed.
- If requirements conflict with existing patterns (e.g., "return entity directly"), flag the conflict and propose the pattern-conforming alternative.
- For security-related changes (JWT validation, CORS, endpoint permissions), ask for confirmation before implementing — these are high-impact.

### Response Style

- Be concise. Lead with the change, not the explanation.
- Use file paths and line references (`UserController.java:37`) when pointing to code.
- Prefer showing diffs or the final state of changed sections, not the full file.
- When creating a new module, list the files to be created before writing any code and confirm the approach.

### When to Ask vs. Assume

| Situation | Action |
| :--- | :--- |
| Task is clear and follows existing patterns | Proceed without asking |
| New module/feature — structure is obvious | List files, then proceed |
| Security changes (auth rules, JWT, CORS) | Always confirm first |
| Schema changes (new table, column rename) | Confirm before writing migration |
| Deleting or refactoring existing modules | Confirm scope and intent |
| Requirements contradict existing patterns | Flag the conflict, propose conforming alternative |

---

## 7. Common Tasks

### Add a New Feature Module

1. Create packages under `src/main/java/com/mb/modules/<feature>/`: `entity/`, `dto/request/`, `dto/response/`, `repository/`, `dao/`, `service/`, `controller/`
2. Create the entity extending `BaseEntity`.
3. Add a DDL migration in `db/changelog/changes/<NNN>-create-<feature>-table.sql` and reference it in `db.changelog-master.yaml` under the Schema migrations section.
4. If the module needs default/reference data, add a DML seed in `db/changelog/seeds/<NNN>-insert-<feature>.sql` and reference it under the Seed data section.
5. Create the Spring Data JPA repository interface.
6. Create DAO interface + `DaoImpl` class.
7. Create service interface + `ServiceImpl` class.
8. Create the controller, using `ApiEndpoint` constants and `ApiResponseBuilder` for all responses.
9. Create `testdata/<Feature>TestDataBuilder.java` in the test tree.
10. Write unit tests for Service and DAO. Write at least one integration test for Controller.

### Add a New Endpoint to an Existing Module

1. Add the path constant to `ApiEndpoint.java`.
2. Add the handler method to the controller (delegate to service, use `responseBuilder`).
3. Add/extend the service method in the interface and implementation.
4. Add/extend the DAO method if a new query is needed.
5. Add unit test for the service method and update the controller unit test.

### Fix a Bug

1. Read the failing test or reproduce the error path.
2. Identify the layer responsible (controller, service, DAO, filter).
3. Fix only the identified code — do not touch adjacent code.
4. Ensure the relevant unit test covers the fixed path. Add a test case if one is missing.

### Add a Database Migration

#### Schema change (DDL)

1. Create `src/main/resources/db/changelog/changes/<NNN>-<description>.sql` (next sequential number).
2. Include a `--rollback` block. For table drops, `DROP TABLE IF EXISTS` is sufficient — PostgreSQL automatically removes all associated indexes and constraints.
3. Add the file reference under the `# Schema migrations (DDL)` section in `db/changelog/db.changelog-master.yaml`.

```sql
--liquibase formatted sql

--changeset author:your-name
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
--rollback ALTER TABLE users DROP COLUMN phone;
```

#### Seed data (DML)

1. Create `src/main/resources/db/changelog/seeds/<NNN>-<description>.sql` (next sequential number within seeds/).
2. Use `ON CONFLICT DO NOTHING` (or equivalent) to make the seed idempotent.
3. Include a `--rollback` block.
4. Add the file reference under the `# Seed data (DML)` section in `db/changelog/db.changelog-master.yaml`.

```sql
--liquibase formatted sql

--changeset author:your-name:NNN-insert-roles
INSERT INTO roles (uuid, name) VALUES (gen_random_uuid(), 'EDITOR')
ON CONFLICT (name) DO NOTHING;
--rollback DELETE FROM roles WHERE name = 'EDITOR';
```

### Refactor Safely

1. Ensure full test coverage exists for the code being refactored before starting.
2. Change one thing at a time — rename, then extract, never both in one step.
3. Run `mvn verify` after each logical step.
4. Do not change public API signatures (method names, parameter types) without updating all callers.

---

## 8. Testing

### Test Types

| Type | Annotation | Dependencies | Speed |
| :--- | :--- | :--- | :---: |
| Unit — Controller | `@WebMvcTest` + `@AutoConfigureRestTestClient` | Service mocked via `@MockitoBean`; security via `MockMvcSecurityConfig` | Fast |
| Unit — Service / DAO | `@ExtendWith(MockitoExtension)` | All mocked | Fast |
| Integration — Controller | extends `AbstractBaseIntegrationTest` + `@AutoConfigureRestTestClient` | Full context + Testcontainers PostgreSQL via `@ServiceConnection`; security via `@AutoConfigureMockMvc` + `MockMvcSecurityConfig` | Slow |
| Repository | extends `AbstractBaseJpaTest` (`@DataJpaTest`) | JPA slice + Testcontainers PostgreSQL via `@ServiceConnection` | Medium |

- Extend `AbstractBaseIntegrationTest` for full-stack controller integration tests. It provides `@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers via `@ServiceConnection`. `@AutoConfigureMockMvc` is required: it activates `MockMvcAutoConfiguration` which creates a `MockMvc` bean; `RestTestClientTestAutoConfiguration` then binds `RestTestClient` to that bean. Without `@AutoConfigureMockMvc`, `RestTestClient` falls back to `bindToApplicationContext()` which builds its own internal MockMvc that ignores all `MockMvcBuilderCustomizer` beans, bypassing the security filter chain entirely.
- Extend `AbstractBaseJpaTest` for repository slice tests. It provides `@DataJpaTest` + Testcontainers + JPA auditing via `TestJpaConfig`.
- Use `UserTestDataBuilder` / `RoleTestDataBuilder` / `UserIdentityTestDataBuilder` for fixtures — do not create test data inline.

### Security Testing in `@WebMvcTest`

Spring Boot 4.x does not automatically apply the Spring Security filter chain to MockMvc. Without explicit wiring, `anyRequest().authenticated()` is never enforced and unauthenticated requests reach the controller. Import `MockMvcSecurityConfig`, `SecurityConfig`, and `CustomAuthenticationEntryPoint` in every `@WebMvcTest` that verifies security behaviour:

```java
@WebMvcTest(controllers = MyController.class)
@AutoConfigureRestTestClient
@Import({ ApiResponseBuilder.class, MockMvcSecurityConfig.class,
          SecurityConfig.class, CustomAuthenticationEntryPoint.class })
class MyControllerTest {
    // @WithMockUser per authenticated test; no annotation for the 401 test
}
```

`MockMvcSecurityConfig` registers a `MockMvcBuilderCustomizer` that calls `apply(springSecurity())`, routing MockMvc through the real `FilterChainProxy`. `SecurityConfig` must be imported to provide the `springSecurityFilterChain` bean. `CustomAuthenticationEntryPoint` must be imported because it is a plain `@Component` not scanned by `@WebMvcTest` but required by `SecurityConfig`.

### Security Testing in `@SpringBootTest` (Integration Tests)

`AbstractBaseIntegrationTest` carries `@AutoConfigureMockMvc` and imports `MockMvcSecurityConfig` — no additional setup is needed in individual integration test classes. The two annotations work together:

- `@AutoConfigureMockMvc` → activates `MockMvcAutoConfiguration` → creates a `MockMvc` bean and collects all `MockMvcBuilderCustomizer` beans in the context
- `MockMvcSecurityConfig` → contributes the customizer that calls `apply(springSecurity())`
- `RestTestClientTestAutoConfiguration` detects the `MockMvc` bean → binds `RestTestClient` to it

In a full `@SpringBootTest` context, `SecurityConfig` and `CustomAuthenticationEntryPoint` are auto-discovered by component scanning — they do **not** need to be explicitly imported (unlike `@WebMvcTest`). Use `@WithMockUser` per test method to satisfy `anyRequest().authenticated()` for tests that are not verifying the 401 path.

### Running Tests

```bash
# All tests
mvn test

# With coverage report
mvn verify
# Report: target/site/jacoco/index.html
```

### Coverage Expectations

- JaCoCo enforces minimum coverage thresholds — the build fails if they are not met.
- Excluded from coverage: DTOs, constants, enums, and `MbSpringRestApplication`.
- Every new service method and DAO method must have a corresponding unit test.
- Every new controller endpoint must have at least one integration test.

### Test Data Builders

```java
// User fixtures
User user = UserTestDataBuilder.buildUser();                         // fixed UUID — use in unit tests
User user = UserTestDataBuilder.buildUser("jane@example.com");       // random UUID — use in integration tests
User user = UserTestDataBuilder.buildUserWithNullNames("j@example.com"); // null first/last name — edge-case tests

// UserIdentity fixtures
UserIdentity local     = UserIdentityTestDataBuilder.buildLocalIdentity(user);        // LOCAL provider, ACTIVE
UserIdentity auth0     = UserIdentityTestDataBuilder.buildAuth0Identity(user);        // AUTH0 provider, ACTIVE
UserIdentity any       = UserIdentityTestDataBuilder.buildIdentity(user, AuthProvider.GOOGLE, "google|xyz"); // arbitrary provider
UserIdentity inactive  = UserIdentityTestDataBuilder.buildInactiveLocalIdentity(user); // LOCAL, INACTIVE — test auth rejection

// Role fixtures
Role admin     = RoleTestDataBuilder.buildAdminRole();
Role user      = RoleTestDataBuilder.buildUserRole();
Role moderator = RoleTestDataBuilder.buildModeratorRole();
```

---

## 9. Deployment

### Local

```bash
docker compose up --build          # Start full stack
docker compose up -d --build       # Detached
docker compose logs -f app         # Follow logs
docker compose down                # Stop
docker compose down -v             # Stop + wipe DB volume
```

### Build Artifacts

```bash
# Full build with all checks
mvn clean verify

# Build JAR only (skip tests)
mvn clean package -DskipTests

# Run JAR directly
java -Dspring.profiles.active=dev -jar target/spring-rest-boilerplate-0.0.1.jar
```

### Environments

| Environment | Profile | Notes |
| :--- | :--- | :--- |
| Local | `local` | Docker Compose or local PostgreSQL |
| Development | `dev` | Shared dev server; env vars injected |
| QA | `qa` | Shared QA server; env vars injected |
| Staging | `stage` | Pre-prod; env vars injected |
| Production | `prod` | All credentials via env vars; SQL logging off |

> For `dev`, `qa`, `stage`, and `prod`: all sensitive values (DB credentials) must be injected via environment variables. No hardcoded values in profile YAMLs.

---

## 10. Security & Best Practices

### Authentication & Authorization

- **JWT-based stateless auth.** `JwtAuthenticationFilter` intercepts all requests. Token parsing/validation logic must be implemented in that filter — there is a `TODO` placeholder.
- **No `HttpSession`** — CSRF protection is disabled by design (token-based APIs do not need it).
- **Custom `AuthenticationEntryPoint`** returns `401 UNAUTHORIZED` as JSON, not a redirect.
- Endpoint access rules are defined in `SecurityConfig` — the pattern is: public actuator health/info, ADMIN-only full actuator, authenticated for everything else.

### Sensitive Data

- Never commit `.env`, credentials, or secrets. `gitleaks` runs on every commit and push.
- Never log request bodies containing passwords, tokens, or PII. `LoggerAspect` logs method arguments — be aware of what is passed to sensitive methods.
- Never expose JPA entities directly from controllers. Always map to response DTOs.
- Use `ExceptionMessage` constants for error messages — avoid leaking internal details (stack traces, SQL errors) in API responses.

### CORS

- Allowed origins are configured per profile in `application-<profile>.yml` under `app.cors.allowed.origins`.
- Update these values when deploying to real environments — the dev/prod defaults are placeholders.
- Allowed headers include: `Authorization`, `Content-Type`, `X-Correlation-Id`, `X-Tenant-Id`, `X-Request-Id`, `X-Api-Version`, `X-Idempotency-Key`.

### Performance Considerations

- The DAO layer abstracts JPA queries — avoid adding complex JPQL/native queries directly in service methods.
- Watch for N+1 query risks when fetching entities with relationships. Use `@EntityGraph` or fetch joins.
- The Apache `RestClient` bean has configurable connection and read timeouts (`app.http.client.*`).

---

## 11. Known Issues / TODOs

- **JWT validation not implemented.** `JwtAuthenticationFilter` has a `TODO` for token parsing and principal extraction. No endpoint is fully secured until this is implemented.
- **Auth service and controller not yet implemented.** `UserIdentity` entity, DAO, and repository are in place under `modules/auth/identity/`. The login flow (provider token verification → identity lookup → issue our JWT) and a `UserIdentityService` + `AuthController` still need to be added. Supported providers defined in `AuthProvider`: `LOCAL`, `AUTH0`, `GOOGLE`, `FACEBOOK`, `GITHUB`.
- **Reference modules are examples, not production code.** `modules/user` and `modules/auth/role` exist to demonstrate patterns. Remove or replace them when starting a real project.
- **Base package rename required.** The default package `com.mb` must be replaced with your organisation's namespace before the boilerplate is used in production.
- **CORS origins are placeholders.** `dev.dummy-frontend-url.com` and `prod.dummy-frontend-url.com` in profile YAMLs must be replaced with real origins.

---

## 12. Contribution Guidelines

### Branch Strategy

- Base all feature branches off `development`.
- Branch naming: `feat/<description>`, `fix/<description>`, `refactor/<description>`, `test/<description>`.
- Direct commits to `main`, `master`, `development`, and `staging` are blocked by pre-commit hooks.

### Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short description>

Types: feat, fix, docs, style, refactor, test, chore, perf
```

Examples:
```
feat(user): add GET endpoint to retrieve user by UUID
fix(auth): correct JWT expiry validation logic
test(user): add integration test for user lookup
chore(deps): upgrade Spring Boot to 4.0.5
```

### Pull Request Checklist

- [ ] `mvn verify` passes (tests, formatting, SpotBugs, PMD, JaCoCo)
- [ ] No hardcoded credentials or secrets
- [ ] New endpoints have unit + integration tests
- [ ] Schema changes include a Liquibase migration with rollback
- [ ] New paths added to `ApiEndpoint.java`
- [ ] Commit messages follow Conventional Commits format
- [ ] PR targets `development`, not `main`

### Pre-commit Hook Summary

| Hook | Trigger | What it checks |
| :--- | :---: | :--- |
| `spotless-apply` | commit | Google Java Format — auto-fixes before commit |
| `spotbugs` | commit | Static analysis (Medium+ bugs fail build) |
| `pmd` | commit | Code quality anti-patterns |
| `unit-tests` | commit | Full test suite |
| `coverage-check` | commit | JaCoCo minimum thresholds |
| `commitizen-check` | commit-msg | Conventional Commits format |
| `gitleaks` | commit + commit-msg | Hardcoded secrets scan |
