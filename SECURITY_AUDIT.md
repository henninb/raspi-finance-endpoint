# Security Audit Report: raspi-finance-endpoint

Audit Date: 2026-02-05 (updated from 2026-02-02)

---

## Changes Since Last Audit (2026-02-02)

**Resolved:**
- Item 6 (Overly permissive catch-all) - Now uses `anyRequest().denyAll()`
- `env.secrets`, `security-test-config.env`, `ssl/` removed from git tracking

**New findings added:** Items 6, 7, 8, 9, 14, 15, 16, 17, 18
**Removed:** IDOR finding - not applicable (single-user/household design, no per-user data ownership in schema)

---

## 1. CRITICAL - Secrets Committed to Git History

**CWE-798** (Use of Hard-coded Credentials), **CWE-312** (Cleartext Storage of Sensitive Information)

**Status: Partially remediated.** `env.secrets`, `security-test-config.env`, and `ssl/` are no longer tracked. However, `.travis.yml` and `env.influx` remain in the working tree and git history still contains all previously committed secrets.

| File | Exposed Data | Tracked? |
|------|-------------|----------|
| `env.secrets` | `JWT_KEY`, `DATASOURCE_PASSWORD`, `INFLUXDB_TOKEN`, `SSL_KEY_PASSWORD` | No (removed) |
| `env.influx` | `DOCKER_INFLUXDB_INIT_PASSWORD`, `DOCKER_INFLUXDB_INIT_ADMIN_TOKEN` | **Yes** |
| `security-test-config.env` | `TEST_USERNAME=henninb@gmail.com`, `TEST_PASSWORD=Monday1!` | No (removed) |
| `.travis.yml:26` | Hardcoded `CREATE USER henninb WITH PASSWORD 'monday1'` | **Yes** |
| `ssl/` directory | Private keys (`*.privkey.pem`) and PKCS12 keystores | No (removed) |

**Remaining remediation:**

1. Remove `.travis.yml` and `env.influx` from git tracking
2. Rotate ALL previously exposed credentials (they remain in git history)
3. Use `git filter-repo` to purge secrets from git history
4. Add a pre-commit hook via `git-secrets` or `gitleaks` to prevent future leaks

---

## 2. CRITICAL - GraphQL Mutations Exempt from CSRF

**CWE-352** (Cross-Site Request Forgery)

`src/main/kotlin/finance/configurations/WebSecurityConfig.kt:84` exempts `/graphql` from CSRF protection:

```kotlin
.ignoringRequestMatchers("/api/login", "/api/register", "/graphiql", "/graphql")
```

Since GraphQL mutations (create/update/delete) go through this endpoint, an attacker can craft a malicious page that submits mutations on behalf of an authenticated user. The JWT cookie with `SameSite=Strict` provides partial mitigation in modern browsers, but older browsers and certain redirect flows bypass SameSite.

**Remediation:** Enforce CSRF on `/graphql` in production. Use a profile-based exemption for development only.

---

## 3. CRITICAL - Fragile Environment Detection for Cookie Security

**CWE-16** (Configuration)

`src/main/kotlin/finance/controllers/LoginController.kt:105-109` determines cookie security flags using:

```kotlin
val isLocalDev = System.getenv("USERNAME")?.contains("henninb") == true
    || System.getenv("HOST_IP")?.contains("192.168") == true
    || activeProfile == "dev"
    || activeProfile == "development"
```

If an attacker can influence the `USERNAME` or `HOST_IP` environment variables (e.g., in a container orchestration scenario), cookies will be set with `secure=false` and `sameSite=Lax`, downgrading security silently. The logic also defaults to the insecure path if `activeProfile` is null.

**Remediation:** Use a dedicated configuration property (e.g., `custom.security.local-dev: true`) instead of fragile env-var sniffing.

---

## 4. CRITICAL - `@CrossOrigin` on Every Controller Bypasses CORS Policy

**CWE-942** (Permissive Cross-domain Policy with Untrusted Domains)

**NEW.** Every controller has a bare `@CrossOrigin` annotation with no origin restriction. This overrides the carefully configured CORS allowlist in `WebSecurityConfig.kt`, defaulting to **all origins allowed**:

```
AccountController.kt:32      CategoryController.kt:28     CsrfController.kt:19
DescriptionController.kt:28  FamilyMemberController.kt:26 LoginController.kt:31
MedicalExpenseController.kt:36 ParameterController.kt:24  PaymentController.kt:27
PendingTransactionController.kt:24 ReceiptImageController.kt:24
TransactionController.kt:34  TransferController.kt:27     UserController.kt:19
ValidationAmountController.kt:27
```

An attacker on any domain can make authenticated cross-origin requests with credentials, completely defeating the centralized CORS allowlist.

**Remediation:** Remove all `@CrossOrigin` annotations from controllers. The centralized `CorsConfiguration` in `WebSecurityConfig` is the correct and sole CORS mechanism.

---

## 5. HIGH - Unvalidated Map Input Bypasses DTO Validation

**CWE-20** (Improper Input Validation)

`src/main/kotlin/finance/controllers/TransactionController.kt:484-505`:

```kotlin
fun changeTransactionAccountNameOwner(
    @RequestBody payload: Map<String, String>,  // No @Valid, no DTO
)
```

This is the only endpoint that accepts a raw `Map` instead of a validated DTO. Extra keys are silently ignored, and values receive only a null check rather than pattern/size validation.

**Remediation:** Replace with a dedicated DTO using `@Valid` and the same `@Pattern` annotations used elsewhere.

---

## 6. HIGH - Unvalidated Base64 Image Upload

**CWE-770** (Allocation of Resources Without Limits), **CWE-434** (Unrestricted Upload)

`src/main/kotlin/finance/controllers/TransactionController.kt:510-523` accepts a raw `@RequestBody String` for image upload with no size limit before Base64 decoding. An attacker can POST a multi-gigabyte string causing OOM before `ImageProcessingService` validation runs.

**Remediation:**

- Add a pre-decode size check: reject if `payload.length > 7_000_000` (approx 5MB after base64)
- Configure `spring.servlet.multipart.max-request-size`
- Validate image magic bytes after decoding
- Restrict MIME types to an explicit allowlist (jpeg, png)

---

## 7. HIGH - Missing `@DecimalMax` on Payment/Transfer Amounts

**CWE-1284** (Improper Validation of Specified Quantity in Input)

**NEW.** `PaymentInputDto.kt:21` and `TransferInputDto.kt:21` have `@DecimalMin("0.01")` but no upper bound:

```kotlin
@field:NotNull
@field:DecimalMin("0.01")
val amount: BigDecimal,  // No @DecimalMax - accepts 999999999999999.99
```

`MedicalExpenseInputDto` correctly has both `@DecimalMin` and `@DecimalMax("999999999.99")`, proving the pattern is known but inconsistently applied.

**Remediation:** Add `@field:DecimalMax("999999999.99")` to both DTOs.

---

## 8. HIGH - Information Disclosure in Error Responses

**CWE-209** (Generation of Error Message Containing Sensitive Information)

**NEW.** Internal exception messages are returned directly to clients throughout the codebase:

```kotlin
// AccountController.kt:371
throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
    "Failed to rename account: ${ex.message}", ex)

// TransactionController.kt:504
throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
    "Failed to update transaction account: ${ex.message}", ex)
```

The `ex` parameter passed to `ResponseStatusException` can expose SQL errors, file paths, class names, and stack traces via Spring's error response.

**Remediation:** Return generic error messages to clients. Log full details server-side with a correlation ID.

---

## 9. HIGH - JWT Cookie maxAge Mismatch

**CWE-613** (Insufficient Session Expiration)

**NEW.** `LoginController.kt:93` creates a JWT valid for 1 hour, but `LoginController.kt:115` sets the cookie to persist for 7 days:

```kotlin
val expiration = Date(now.time + 60 * 60 * 1000)  // 1 hour JWT
// ...
.maxAge(7 * 24 * 60 * 60)  // 7 day cookie
```

The registration endpoint uses 24-hour `maxAge`, creating additional inconsistency. While expired JWTs are rejected, the stale cookie persists and a stolen cookie provides a wider reconnaissance window.

**Remediation:** Align `maxAge` with JWT expiration. Implement refresh tokens if longer sessions are needed.

---

## 10. HIGH - Actuator Endpoints Fully Exposed

**CWE-200** (Exposure of Sensitive Information)

**NEW.** `src/main/resources/application-prod.yml:216`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"
```

While `/actuator/**` requires authentication (line 93 in `WebSecurityConfig`), `include: "*"` exposes `/actuator/env` (environment variables including DB passwords), `/actuator/configprops`, and `/actuator/threaddump`. If the JWT filter is bypassed, this is full information disclosure.

**Remediation:** Restrict to `include: "health,metrics,info"`.

---

## 11. HIGH - `unique = true` on Password Column

**CWE-204** (Observable Response Discrepancy)

**NEW.** `src/main/kotlin/finance/domain/User.kt:62`:

```kotlin
@Column(name = "password", unique = true, nullable = false)
var password: String,
```

A unique constraint on the hashed password column is semantically wrong. While BCrypt salts make collisions extremely unlikely, a `DataIntegrityViolationException` during registration would reveal that another user has the same password hash. The constraint serves no purpose and should be removed.

**Remediation:** Remove `unique = true` from the password column. Update the corresponding Flyway migration.

---

## 12. HIGH - In-Memory Token Blacklist (Single Instance Only)

**CWE-613** (Insufficient Session Expiration)

`src/main/kotlin/finance/services/TokenBlacklistService.kt` stores blacklisted tokens in a `ConcurrentHashMap`. In a multi-instance deployment, a token revoked on one instance remains valid on others. Blacklist is lost on application restart.

**Remediation:** Use a shared store (Redis, database) for the blacklist, or switch to short-lived tokens with a refresh token flow.

---

## 13. HIGH - CSRF Cookie HttpOnly Disabled

**CWE-1004** (Sensitive Cookie Without HttpOnly Flag)

`src/main/kotlin/finance/configurations/WebSecurityConfig.kt:75`:

```kotlin
CookieCsrfTokenRepository.withHttpOnlyFalse()
```

The CSRF token cookie is readable by JavaScript. This is an intentional design choice for SPA clients but means any XSS vulnerability allows CSRF token theft.

**Remediation:** Use `httpOnly=true` and have the frontend fetch the token via the `/api/csrf` endpoint instead of reading the cookie directly.

---

## 14. HIGH - GraphQL Introspection Not Disabled in Production

**CWE-200** (Exposure of Sensitive Information)

**NEW.** No configuration disabling GraphQL introspection was found in any application YAML or Kotlin configuration. GraphQL introspection allows attackers to enumerate all queries, mutations, field types, and relationships.

Note: `graphiql.enabled: false` is correctly set for production, but this only disables the UI - the introspection query endpoint remains accessible.

**Remediation:** Disable introspection in production via Spring GraphQL configuration or a custom interceptor that blocks `__schema` and `__type` queries.

---

## 15. MEDIUM - Unvalidated Path Parameters

**CWE-20** (Improper Input Validation)

**NEW.** Path parameters across controllers lack validation constraints, bypassing the DTO validation layer:

```kotlin
// TransactionController.kt:541
@PathVariable("category_name") categoryName: String,  // No @Pattern, @Size

// AccountController.kt:379-380
@RequestParam(value = "old") oldAccountNameOwner: String,  // No validation
@RequestParam("new") newAccountNameOwner: String,          // No validation
```

While JPA parameterized queries prevent SQL injection, unvalidated strings can cause log injection, application errors from unexpected characters, and DoS from oversized values.

**Remediation:** Add `@Pattern` and `@Size` to all `@PathVariable` and `@RequestParam` parameters.

---

## 16. MEDIUM - Missing `@Transactional` on Multi-Step Operations

**CWE-362** (Race Condition)

**NEW.** Only `PaymentService.insertPayment()` has `@Transactional`. The standardized `PaymentService.save()` method creates multiple transactions across tables without a transactional boundary. If it fails midway, partial records remain.

**Remediation:** Add `@Transactional` to `PaymentService.save()` and review other multi-step service methods.

---

## 17. MEDIUM - Sensitive Usernames in Logs

**CWE-532** (Insertion of Sensitive Information into Log File)

**NEW.** `LoginController.kt:85,87,129`:

```kotlin
logger.info("LOGIN_AUTH_ATTEMPT username=${loginRequest.username}")
logger.warn("LOGIN_401_INVALID_CREDENTIALS username=${loginRequest.username}")
logger.info("LOGIN_SUCCESS username=${loginRequest.username}")
```

Plaintext usernames in logs create PII exposure risk. The `HttpErrorLoggingFilter` correctly sanitizes headers but controller-level logs bypass this.

**Remediation:** Hash or mask usernames in logs. Use a structured logging approach with a sensitive-data filter.

---

## 18. MEDIUM - No Role-Based Access Control

**CWE-862** (Missing Authorization)

`JwtAuthenticationFilter.kt:107-111` grants every authenticated user `ROLE_USER` and `USER`. `@PreAuthorize` checks only `hasAuthority('USER')` which every user has. There is no admin/viewer/editor distinction.

**Remediation:** If multi-tenancy or privilege separation is needed, implement role hierarchy. Otherwise, document this as an intentional single-role design.

---

## 19. MEDIUM - Rate Limit Configuration Mismatch

`CLAUDE.md` documents 5000 RPM. `RateLimitingFilter.kt:21` defaults to 500. Rate limiting is in-memory only (won't work across instances), uses fixed-window counting (allows burst attacks), and applies uniformly to all endpoints.

**Remediation:** Align documented and actual defaults. Add per-endpoint limits for `/api/login` (e.g., 5/minute). Consider sliding-window algorithm.

---

## 20. MEDIUM - Broad CORS Origin List

`src/main/kotlin/finance/configurations/WebSecurityConfig.kt:138-155` allows 10+ origins including `http://localhost:3000` and multiple subdomains. Combined with `allowCredentials=true`, a compromised origin can make authenticated cross-origin requests.

Note: This finding is compounded by item 4 (`@CrossOrigin` on every controller) which bypasses this allowlist entirely.

**Remediation:** Use profile-based CORS configuration. Restrict `localhost` origins to development profiles only. Fix item 4 first.

---

## 21. LOW - Missing Security Headers

`WebSecurityConfig.kt:50-64` sets good headers but is missing:

- `Permissions-Policy` (restricts browser features like camera, geolocation)
- `X-Permitted-Cross-Domain-Policies: none`

---

## 22. LOW - Personal Information Exposure

- Email `henninb@msn.com` in `OpenApiConfig.kt:27` and `.travis.yml:43`
- Personal domain names hardcoded throughout CORS and cookie config

---

## Dependency CVE Assessment

| Dependency | Version | Known CVEs | Status |
|-----------|---------|-----------|--------|
| Spring Boot | 4.0.1 | None for 4.x | **Safe** |
| Spring Security | 7.0.2 | CVE-2025-41248 affects 6.4-6.5 only | **Safe** |
| PostgreSQL JDBC | 42.7.9 | CVE-2025-49146 affects 42.7.4-42.7.6 | **Safe** (fixed in 42.7.7) |
| H2 | 2.4.240 | All known CVEs affect <2.1.210 | **Safe** |
| Jackson Databind | 2.21.0 | Polymorphic deser CVEs affect <2.10 | **Safe** |
| JJWT | 0.13.0 | CVE-2024-31033 disputed/rejected | **Safe** |
| Flyway | 11.20.3 | No known CVEs | **Safe** |
| Log4j Core | 2.25.3 | Log4Shell affects <2.17.0 | **Safe** |
| Resilience4j | 2.3.0 | No known CVEs | **Safe** |
| Logback | 1.5.26 | No known CVEs for 1.5.x | **Safe** |

The project has OWASP Dependency-Check configured (`build.gradle` lines 109-113) with a fail threshold of CVSS 7.0. All dependency versions are current and not affected by known CVEs.

---

## SQL Injection Assessment

All native SQL queries use `@Param` bindings. No string concatenation, no raw JDBC, no dynamic query building found. **No SQL injection vulnerabilities detected.**

---

## Positive Security Findings

- BCrypt password hashing with timing-attack prevention (dummy hash for non-existent users)
- Stateless JWT with proper HMAC-SHA signature verification (no algorithm confusion)
- HttpOnly + Secure + SameSite=Strict cookies for auth tokens in production
- HSTS with preload in production (6 months, including subdomains)
- Strong CSP (`default-src 'none'`) for API
- GraphQL query depth (12) and complexity (300) limits via `GraphQLGuardrailsConfig`
- Default-deny authorization (`anyRequest().denyAll()`)
- Comprehensive audit logging with sensitive header/parameter sanitization
- IP address validation with proxy-header trust restriction to private networks
- OWASP Dependency-Check enabled in build pipeline
- Dependency locking enabled for reproducible builds
- XOR-encoded CSRF tokens (BREACH attack protection via `XorCsrfTokenRequestAttributeHandler`)
- Image format whitelist (JPEG/PNG only) and size limits at database level (`ck_image_size`)
- Soft delete pattern prevents accidental data loss
- Pre-encoded password rejection prevents BCrypt bypass attempts

---

## Summary

| Severity | Count | Items |
|----------|-------|-------|
| CRITICAL | 4 | 1, 2, 3, 4 |
| HIGH | 10 | 5, 6, 7, 8, 9, 10, 11, 12, 13, 14 |
| MEDIUM | 6 | 15, 16, 17, 18, 19, 20 |
| LOW | 2 | 21, 22 |

---

## Priority Remediation Order

1. **Remove `@CrossOrigin`** from all controllers (item 4) - quick fix, massive impact
2. **Rotate all secrets** and purge git history (item 1)
3. **Restrict actuator exposure** to `health,metrics,info` (item 10)
4. **Add `@DecimalMax`** to PaymentInputDto/TransferInputDto (item 7) - one-line fix
5. **Remove `unique=true`** from password column (item 11)
6. **Replace Map endpoint** with typed DTO (item 5)
7. **Fix environment detection** for cookie security (item 3)
8. **Enforce CSRF on GraphQL** in production (item 2)
9. **Sanitize error responses** - stop leaking internal details (item 8)
10. **Align cookie maxAge** with JWT expiration (item 9)
11. **Disable GraphQL introspection** in production (item 14)
12. **Add image upload size validation** before Base64 decode (item 6)
13. **Add `@Pattern`/`@Size`** to all path variables (item 15)
14. Address remaining medium/low items
