**Recommendations**

- Architecture
  - Unify on annotation-based GraphQL controllers:
    - Move mutations to `@MutationMapping` methods in `GraphQLQueryController` o
r dedicated `*GraphqlController` classes.
    - Remove or slim `DataFetcher`-based resolvers once migrated.
    - Keep `RuntimeWiringConfigurer` only for scalars and instrumentation.
  - Align schema to implementation:
    - Remove or temporarily comment out unused schema fields/mutations you don’t
 implement yet to avoid misleading clients.

- Security
  - Protect GraphQL route:
    - In `WebSecurityConfig`, add `.requestMatchers("/graphql").authenticated()`
 and restrict roles as appropriate for both queries and mutations.
    - Keep `@PreAuthorize` on mutation handlers, but don’t rely on it exclusivel
y.
  - Lock down GraphiQL:
    - Set `spring.graphql.graphiql.enabled=false` in `application-prod*.yml` and
 `application-stage.yml`.
  - CORS consistency:
    - Remove `spring.graphql.cors.*` wildcard config from `application.yml` and
centralize CORS in `WebSecurityConfig.corsConfigurationSource()` with your white
listed domains.

- Performance & Resilience
  - Add query guardrails (instrumentation):
    - Register max depth and complexity instrumentations via `RuntimeWiringConfi
gurer` or a `GraphQlSourceBuilderCustomizer` bean.
    - Add a per-request timeout for execution with appropriate error mapping.
  - Introduce DataLoader/batching:
    - Use `@BatchMapping` or DataLoader for nested associations (e.g., resolving
 `ReceiptImage` for transactions).
  - Pagination and filtering:
    - Replace list-returning fields with pagination patterns: add `first/after`
or simple `limit/offset` with sorting and filters.
    - Consider Relay-style Connections where appropriate.

- Schema & Types
  - Prefer `ID` for identifiers in schema instead of `Long` for portability with
 clients; map to `Long` internally.
  - Use Spring’s built-in scalars where possible:
    - Consider `ExtendedScalars.Date`/`DateTime` instead of custom `SqlDateScala
r`/`TimestampScalar` unless you need strict interop.
  - Input DTOs and validation:
    - Replace manual `ObjectMapper` conversion with typed `@Argument` DTOs (e.g.
, `PaymentInputDto`) annotated with `javax.validation` constraints (`@NotBlank`,
 `@Positive`, etc.).
    - Validate inputs via `@Validated` on controller and surface field-level Gra
phQL errors with extensions.

- Error Handling & Observability
  - Add a GraphQL exception handler:
    - Provide `@ControllerAdvice` with `@GraphQlExceptionHandler` to translate d
omain exceptions into GraphQL errors with `ErrorType` and `extensions`.
  - Reduce noisy logging in prod:
    - Downgrade request/response logging for GraphQL to DEBUG and sanitize varia
ble values; keep counts and error summaries at INFO/WARN.
  - Metrics:
    - Consider enabling Spring Observability for GraphQL to auto-collect query t
imings and error counts; keep your custom counters but align names with a consis
tent metric namespace.

- Testing
  - Add `WebGraphQlTester` tests:
    - Cover representative queries and mutations end-to-end, including security
contexts and validation failures.
    - Validate pagination, filters, and error shapes.
  - Remove or refactor resolver-only tests after moving to `@MutationMapping` to
    target controller methods or GraphQL endpoint tests.

- Configuration & Build Hygiene
  - Dependency cleanup:
    - Remove duplicate `spring-boot-starter-graphql` entries from `build.gradle`
 to avoid version drift; keep a single managed dependency.
  - Profile-specific overrides:
    - Ensure `schema.printer.enabled=true` and GraphiQL are disabled in prod/sta
ge profiles.
  - Remove legacy artifacts:
    - Delete `GraphqlProvider.kt` entirely (it’s a stubbed legacy file now).
    - Reconcile `.graphqlconfig` endpoint host with current environments or move
 it under tooling-only config.
**GraphQL Modernization Plan (Spring Boot 4.0)**

**Objectives**
- Align schema and resolvers with current functionality and domain.
- Add production guardrails (depth/complexity limits, timeouts, batching).
- Improve developer ergonomics (testing, DTOs, observability, logging hygiene).
- Simplify stack and configuration for clearer ops and security posture.

**Current State Summary**
- Schema-first with `src/main/resources/graphql/schema.graphqls` and annotation-based resolvers (`@QueryMapping`, `@MutationMapping`).
- Custom scalars wired via `RuntimeWiringConfigurer`; global exception mapping and validation in place.
- `/graphql` gated by Spring Security; GraphiQL disabled in prod/stage.
- Missing guardrails (depth/complexity/timeout), batching, and WebGraphQlTester coverage; several stubbed queries; mixed MVC/WebFlux dependencies.

**Recommendations**
- Schema & Types
  - Prefer `ID`: use `ID` for identifiers in the SDL; map to `Long` internally.
  - Adopt `java.time`: migrate to `LocalDate` and `Instant/OffsetDateTime` with `ExtendedScalars.Date`/`DateTime` or Spring’s Java Time scalars; retire `SqlDateScalar`/`TimestampScalar` if not strictly needed.
  - Remove drift: delete or comment out unimplemented queries/mutations (e.g., `transactions`, `parameters`, `validationAmounts`, `receiptImages`) until implemented.

- Controllers & Resolvers
  - Batching: introduce DataLoader/`@BatchMapping` for nested fields (e.g., `Transaction.receiptImage`) to avoid N+1s.
  - Pagination/filtering: replace unbounded lists with `limit/offset` (or Relay connections) and add filters/sort where appropriate.
  - DTO boundaries: continue using dedicated input DTOs with validation; avoid exposing entities directly in mutations.

- Security
  - Keep `/graphql` authenticated; restrict mutations with fine-grained authorities using `@PreAuthorize`.
  - Consider introspection policy in prod if API is private (instrumentation-based disablement optional).

- Guardrails & Performance
  - Depth/complexity: register `MaxQueryDepthInstrumentation` and `MaxQueryComplexityInstrumentation` via a `GraphQlSourceBuilderCustomizer` bean; tune thresholds to your schema.
  - Timeouts: apply per-request timeouts around service calls (e.g., Resilience4j `TimeLimiter`) or Reactor timeouts if reactive.
  - Caching: add small caches for reference/rarely-changing lookups where safe.

- Observability & Logging
  - Logging hygiene: reduce GraphQL interceptor logs to DEBUG; avoid logging full queries/variables in INFO/WARN/ERROR; sanitize sensitive values.
  - Metrics: enable/standardize Micrometer observations for GraphQL queries/mutations and errors; retain domain-specific counters with a consistent prefix (e.g., `graphql.payment.*`).
  - Error shape: enrich errors with structured `extensions` (e.g., `errorCode`, `field`, `timestamp`).

- Testing
  - Add `WebGraphQlTester` coverage: authenticated/unauthenticated paths, validation failures, pagination/filters, and nested resolvers with DataLoader.
  - Keep/update scalar tests; add negative cases for malformed inputs.

- Configuration & Build
  - Choose stack: remove `spring-boot-starter-webflux` if running on Servlet/MVC only.
  - Disable schema printer in prod/stage unless actively used.
  - GraphiQL: keep enabled only for dev/test.
  - `.graphqlconfig`: point `schemaPath` to `src/main/resources/graphql/schema.graphqls` for IDE tooling, or mark it tooling-only.

**Concrete Changes (Action List)**
- Schema & Types
  - Switch `Long` identifiers to `ID` in `src/main/resources/graphql/schema.graphqls`.
  - Replace custom `Date`/`Timestamp` scalars with Java Time scalars and adjust DTOs/entities to `LocalDate`/`Instant`.
  - Remove or comment unimplemented fields/mutations from the schema and controllers.

- Guardrails
  - Add `@Bean` `GraphQlSourceBuilderCustomizer` registering depth/complexity instrumentations.
  - Introduce per-request execution timeout at service layer boundaries.

- Batching
  - Define DataLoader(s) and a `DataLoaderRegistry` bean; implement `@BatchMapping` for common N+1 paths.

- Logging & Metrics
  - Update `GraphQLInterceptor` to guard with `logger.isDebugEnabled()` and sanitize outputs; keep summaries at INFO and details at DEBUG.
  - Add standardized GraphQL metrics (timings, error counts) via Micrometer Observations.

- Testing
  - Create `WebGraphQlTester` tests for: accounts/categories/descriptions queries; payment/transfer create/delete mutations; validation and authorization failures.

- Build & Config
  - Remove `implementation("org.springframework.boot:spring-boot-starter-webflux")` from `build.gradle` unless reactive endpoints are required.
  - Ensure `spring.graphql.schema.printer.enabled=false` in prod/stage profiles.

**Optional Enhancements**
- Cursor-based pagination (Relay connections) for high-cardinality types.
- Response caching for idempotent queries behind auth where appropriate.
- Persisted queries/allowlist if clients are controlled and security posture requires it.

**Acceptance Criteria**
- All GraphQL endpoints pass `WebGraphQlTester` tests (happy paths, auth, validation, and error mapping).
- No unimplemented or stubbed fields exposed in the schema.
- Depth/complexity guards and timeouts active; production logs free of raw query payloads/PII.
- No N+1 warnings during nested queries; DataLoader batch sizes observable in metrics.
- Build uses a single web stack (MVC or WebFlux) and prod/stage disable GraphiQL and schema printer.
