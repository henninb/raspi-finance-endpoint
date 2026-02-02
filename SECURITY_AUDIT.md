# Security Audit Report: raspi-finance-endpoint

Audit Date: 2026-02-02

---

## 1. CRITICAL - Secrets Committed to Git History

**CWE-798** (Use of Hard-coded Credentials), **CWE-312** (Cleartext Storage of Sensitive Information)

Multiple files containing production credentials are tracked in git:

| File | Exposed Data |
|------|-------------|
| `env.secrets` | `JWT_KEY`, `DATASOURCE_PASSWORD`, `INFLUXDB_TOKEN`, `SSL_KEY_PASSWORD` (all set to weak values like `monday1`) |
| `env.influx` | `DOCKER_INFLUXDB_INIT_PASSWORD`, `DOCKER_INFLUXDB_INIT_ADMIN_TOKEN` |
| `security-test-config.env` | `TEST_USERNAME=henninb@gmail.com`, `TEST_PASSWORD=Monday1!` |
| `.travis.yml:26` | Hardcoded `CREATE USER henninb WITH PASSWORD 'monday1'` |
| `ssl/` directory | Private keys (`*.privkey.pem`) and PKCS12 keystores |

The `.gitignore` has rules for these files, but they are already in git history. Adding a `.gitignore` rule after a file is committed does not remove it from history.

**Remediation:**

1. Rotate ALL exposed credentials immediately (JWT key, database passwords, InfluxDB tokens, SSL certificates)
2. Use `git filter-repo` to purge secrets from git history
3. Add a pre-commit hook via `git-secrets` or `gitleaks` to prevent future leaks

---

## 2. CRITICAL - GraphQL Mutations Exempt from CSRF

**CWE-352** (Cross-Site Request Forgery)

`src/main/kotlin/finance/configurations/WebSecurityConfig.kt` exempts `/graphql` from CSRF protection. Since GraphQL mutations (create/update/delete) go through this endpoint, an attacker can craft a malicious page that submits mutations on behalf of an authenticated user.

**Remediation:** Enforce CSRF on `/graphql` in production. Use a profile-based exemption for development only.

---

## 3. CRITICAL - Fragile Environment Detection for Cookie Security

**CWE-16** (Configuration)

`src/main/kotlin/finance/controllers/LoginController.kt:105-109` determines cookie security flags using:

```kotlin
val isLocalDev = System.getenv("USERNAME")?.contains("henninb") == true
    || System.getenv("HOST_IP")?.contains("192.168") == true
    || ...
```

If these environment variables are absent or misconfigured in production, cookies will be set with `secure=false` and `sameSite=Lax`, downgrading security silently.

**Remediation:** Use Spring profiles exclusively (`@Value("${spring.profiles.active}")`) instead of fragile env-var sniffing.

---

## 4. HIGH - Unvalidated Map Input Bypasses DTO Validation

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

## 5. HIGH - Unvalidated Base64 Image Upload

**CWE-770** (Allocation of Resources Without Limits), **CWE-434** (Unrestricted Upload)

`src/main/kotlin/finance/controllers/TransactionController.kt:510-523` accepts a raw `@RequestBody String` for image upload with no size limit, no MIME type whitelist, and no magic-number validation. The regex stripping the data URI prefix (`^data:image/[a-z]+;base64,`) accepts any alphabetic MIME subtype.

`src/main/kotlin/finance/services/TransactionService.kt:336-370` decodes and processes the image without bounds checking.

**Remediation:**

- Add a `@Size(max = ...)` or configure `spring.servlet.multipart.max-request-size`
- Validate image magic bytes after decoding
- Restrict MIME types to an explicit allowlist (jpeg, png, webp)

---

## 6. HIGH - Overly Permissive Catch-All Authorization

**CWE-862** (Missing Authorization)

`src/main/kotlin/finance/configurations/WebSecurityConfig.kt:90`:

```kotlin
auth.anyRequest().permitAll()
```

Any endpoint not explicitly matched above is publicly accessible. If a new controller is added without updating this config, it will be unprotected by default.

**Remediation:** Change to `auth.anyRequest().denyAll()` or `auth.anyRequest().authenticated()` and explicitly permit only known public paths.

---

## 7. HIGH - In-Memory Token Blacklist (Single Instance Only)

**CWE-613** (Insufficient Session Expiration)

`src/main/kotlin/finance/services/TokenBlacklistService.kt` stores blacklisted tokens in a `ConcurrentHashMap`. In a multi-instance deployment behind a load balancer, a token revoked on one instance remains valid on others.

**Remediation:** Use a shared store (Redis, database) for the blacklist, or switch to short-lived tokens with a refresh token flow.

---

## 8. HIGH - CSRF Cookie HttpOnly Disabled

**CWE-1004** (Sensitive Cookie Without HttpOnly Flag)

`src/main/kotlin/finance/configurations/WebSecurityConfig.kt:75`:

```kotlin
CookieCsrfTokenRepository.withHttpOnlyFalse()
```

The CSRF token cookie is readable by JavaScript. If an XSS vulnerability exists anywhere in the frontend, the attacker can read the CSRF token and bypass CSRF protection entirely.

**Remediation:** Use `httpOnly=true` and have the frontend fetch the token via the `/api/csrf` endpoint instead of reading the cookie directly.

---

## 9. MEDIUM - No Role-Based Access Control

**CWE-862** (Missing Authorization)

`JwtAuthenticationFilter.kt:107-111` grants every authenticated user `ROLE_USER` and `USER`. `@EnableMethodSecurity` is enabled but `@PreAuthorize` is used only on GraphQL mutations with `hasAuthority('USER')` which every user has. There is no admin/viewer/editor distinction.

**Remediation:** If multi-tenancy or privilege separation is needed, implement role hierarchy. Otherwise, document this as an intentional single-role design.

---

## 10. MEDIUM - Rate Limit Configuration Mismatch

`CLAUDE.md` documents 5000 RPM. `RateLimitingFilter.kt:21` defaults to 500. Additionally, rate limiting is in-memory only (won't work across multiple instances) and applies uniformly to all endpoints. Login should have stricter limits than data queries.

**Remediation:** Align the documented and actual defaults. Add per-endpoint rate limits, especially for `/api/login` and `/api/register`.

---

## 11. MEDIUM - Broad CORS Origin List

`src/main/kotlin/finance/configurations/WebSecurityConfig.kt:138-155` allows 10+ origins including `http://localhost:3000` and multiple subdomains. Combined with `allowCredentials=true`, a compromised origin can make authenticated cross-origin requests.

**Remediation:** Use profile-based CORS configuration. Restrict `localhost` origins to development profiles only.

---

## 12. LOW - Missing Security Headers

`WebSecurityConfig.kt:50-64` sets good headers but is missing:

- `Permissions-Policy` (restricts browser features like camera, geolocation)
- `X-Permitted-Cross-Domain-Policies: none`

---

## 13. LOW - Personal Information Exposure

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

All 8 native SQL queries use `@Param` bindings. No string concatenation, no raw JDBC, no dynamic query building found. **No SQL injection vulnerabilities detected.**

---

## Positive Security Findings

- BCrypt password hashing with timing-attack prevention (dummy hash for non-existent users)
- Stateless JWT with proper signature verification
- HttpOnly + Secure + SameSite=Strict cookies for auth tokens
- HSTS with preload in production
- Strong CSP (`default-src 'none'`) for API
- GraphQL query depth (12) and complexity (300) limits
- Comprehensive audit logging with sensitive data sanitization
- IP address validation with proxy-header trust restriction to private networks
- OWASP Dependency-Check enabled in build pipeline
- Dependency locking enabled for reproducible builds

---

## Priority Remediation Order

1. **Rotate all secrets** and purge git history (items 1)
2. **Fix authorization catch-all** to deny by default (item 6)
3. **Enforce CSRF on GraphQL** in production (item 2)
4. **Fix environment detection** for cookie security (item 3)
5. **Add DTO validation** for the Map endpoint (item 4)
6. **Add image upload validation** (item 5)
7. Address remaining medium/low items
