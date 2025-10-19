# Security Analysis Report

This document contains a comprehensive security assessment of the raspi-finance-endpoint application, focusing on the `/api/login` endpoint vulnerabilities and remediation strategies.

## Additional Hardening Opportunities (2025-10-19)

The items below complement the existing findings with concrete, high‑impact steps to reduce OWASP Top 10 risk, improve zero‑day readiness, protect APIs, and manage CVEs/supply‑chain exposure.

### 0. Supply Chain & CVE Management
- Pin to GA releases: Avoid milestone/RC builds in production (e.g., `springBootVersion=4.0.0-M3`, `springSecurityVersion=7.0.0-M3`, `micrometerInfluxRegistryVersion=1.16.0-RC1`, `jacksonAnnotationsVersion=3.0-rc5`). Track the latest stable Boot line (e.g., 3.3.x/3.4.x) and align ecosystem versions via the Spring Boot BOM.
- Remove unnecessary surfaces: Drop `org.apache.logging.log4j:log4j-core` unless required; project already uses Logback. Reduces attack surface and classpath conflicts.
- Enable dependency locking: Use Gradle dependency locking to freeze transitive versions and prevent drift.
- Automate CVE scanning: Add OWASP Dependency-Check and/or Grype/Trivy to CI for Gradle and container images. Fail PRs on high/critical CVEs.
- Keep JJWT current: Stay on latest JJWT 0.13.x+ and prefer small, frequent upgrades to minimize blast radius.

Example Gradle plugin (documented; add when ready):
```gradle
plugins {
  id "org.owasp.dependencycheck" version "9.2.0"
}

dependencyCheck {
  failBuildOnCVSS = 7.0
  suppressionFile = "config/dependencycheck/suppressions.xml" // optional
}

tasks.named('check') { dependsOn 'dependencyCheckAnalyze' }
```

### 1. Security Headers (A05:2021 – Security Misconfiguration)
- Enforce HSTS in prod: `Strict-Transport-Security: max-age=15552000; includeSubDomains; preload`.
- Set standard headers via Spring Security: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, a conservative `Referrer-Policy`, and a minimal CSP (even for APIs) to guard default error pages.
- Prefer SecurityFilterChain headers API over a custom filter to centralize policy.

Suggested snippet (WebSecurityConfig):
```kotlin
http
  .headers { headers ->
    headers.contentTypeOptions { }
    headers.frameOptions { it.deny() }
    headers.referrerPolicy { it.policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
    headers.httpStrictTransportSecurity { it.includeSubDomains(true).preload(true).maxAgeInSeconds(15552000) }
    headers.contentSecurityPolicy { csp -> csp.policyDirectives("default-src 'none'") }
  }
```

### 2. Logging Hygiene (A09:2021 – Security Logging & Monitoring Failures)
- Disable body logging in prod: `RequestLoggingFilter` currently logs request bodies. Restrict to `dev` only and redact sensitive keys if ever enabled. Consider removing entirely on public endpoints.
- Downgrade sensitive logs: Success auth logs with username/IP at `INFO` can leak PII in aggregated logs. Consider `DEBUG` for success; keep failures at `WARN` with redaction/truncation (already partially implemented).
- Never log secrets/tokens: Continue sanitizing headers/params; ensure any new filters avoid cookies, `Authorization`, keys, or JWTs in logs.

Dev-only filter example:
```kotlin
@Profile("dev")
@Component
class RequestLoggingFilter : OncePerRequestFilter() { /* redacted body logging */ }
```

### 3. CORS Policy Tightening (A05:2021 – Security Misconfiguration)
- Externalize allowed origins: Replace hardcoded origins in `WebSecurityConfig.corsConfigurationSource()` with profile‑driven lists. Maintain a minimal prod allowlist; remove `chrome-extension://...` from prod.
- Avoid wildcards and mixed schemes; prefer `https://<exact-host>`.

### 4. Actuator Hardening (A05:2021)
- Production: limit exposure to `health`, `info` and require auth; disable details on `health` unless behind auth. Keep “include *” only in local/dev.
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: when_authorized
```

### 5. GraphQL Guardrails (A01/A04/A06)
- Move depth/complexity limits to config: Keep defaults conservative and allow tightening in prod (e.g., depth 8–10, complexity ~150–200).
- Disable GraphiQL in prod: Already configurable; ensure prod profile sets `graphiql.enabled: false`.
- Consider allowlisted persisted queries in prod to reduce attack surface.

### 6. Rate Limiting & Abuse Controls (A07:2021 – Identification & Authentication Failures)
- Current filter improves trust of proxy headers; extend limiting dimensions: per IP, per user, and per credential source (cookie vs header).
- Introduce endpoint‑specific throttles: Stricter for `/api/login`, `/graphql` mutations, and write endpoints.
- Consider a proven library (e.g., Bucket4j) with token buckets and distributed backing (Redis) if running multi‑instance.

### 7. API Protection (A01/A02/A05)
- Optional API key guard: Implement an interceptor to enforce a static or rotated API key for non‑browser use cases; scope to specific routes (e.g., admin/automation). Keep disabled by default.
- Uniform auth across REST and GraphQL: Ensure JWT cookie/Authorization header paths are consistent and CSRF‑resistant (SameSite=Strict is in place).

### 8. JWT Hardening (A07/A02)
- Key rotation: Implement dual‑key validation (current + previous) and put a `kid` header in tokens to streamline rollovers.
- Shorter lifetimes for privileged ops: Keep general tokens short (e.g., 30–60 min) and consider refresh tokens for longer sessions if needed.
- Blacklist on logout (optional): For high‑risk users/flows, maintain a short‑lived denylist until token expiry.

### 9. Zero‑Day Readiness
- CI policy gates: Fail builds on high/critical CVEs; surface SBOMs (CycloneDX) for quick impact analysis.
- Runtime posture: Keep defense‑in‑depth (headers, CORS, strict auth, minimal logs) to mitigate exploitability of unknowns.
- Fast upgrades: Use Dependabot (or Renovate) for Gradle and Docker to shorten patch lead time.

Example `.github/dependabot.yml` (documented):
```yaml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "daily"
    open-pull-requests-limit: 5
  - package-ecosystem: "docker"
    directory: "/"
    schedule:
      interval: "daily"
```

### 10. Quick OWASP Top 10 Alignment
- A01 Broken Access Control: Enforce method security (`@PreAuthorize`), double‑check all admin‑level routes, and avoid controller over‑exposure.
- A02 Cryptographic Failures: Enforce TLS, HSTS, strong JWT keys, and avoid legacy ciphers.
- A03 Injection: Continue using JPA/validation; validate GraphQL inputs and IDs/UUIDs.
- A04 Insecure Design: Keep rate limiting/account lockout and secure defaults.
- A05 Security Misconfiguration: Headers, CORS, actuator, and minimal services.
- A06 Vulnerable & Outdated Components: CVE scanning, GA releases, BOM alignment, dependency locking.
- A07 Identification & Auth Failures: MFA option, lockout, token rotation, cookie security.
- A08 Software/Data Integrity: Sign images (Sigstore/Cosign), verify artifact provenance.
- A09 Security Logging/Monitoring: Redacted, structured logs; metrics + alerts.
- A10 Server-Side Request Forgery: Avoid proxying arbitrary URLs; whitelist outbound targets if added.

—
The sections below remain from the earlier assessment and continue to apply.

## RED TEAM ASSESSMENT: /api/login ENDPOINT

### CRITICAL VULNERABILITIES IDENTIFIED ⚠️

#### 1. Information Disclosure - Password Logging (RESOLVED ✅)
**Location**: `UserService.kt:23`
**Status**: FIXED
**Original Issue**:
```kotlin
logger.info("user-pass: ${user.password}")
```
**Impact**: Plaintext passwords were logged to application logs
**Resolution**: Password logging completely removed and constant-time authentication implemented

#### 2. Timing Attack Vulnerability (RESOLVED ✅)
**Location**: `LoginController.kt:50-60`
**Status**: FIXED
**Original Issue**: Different response times for valid vs invalid usernames allowed username enumeration
**Resolution**: Implemented constant-time authentication with dummy password hashing for non-existent users

#### 3. Information Leakage via Cookie Settings (RESOLVED ✅)
**Location**: `LoginController.kt:81-82`
**Status**: FIXED
**Original Issue**:
```kotlin
.httpOnly(false) // Allow JavaScript access in development for debugging
.secure(false) // Never require HTTPS for local development
```
**Impact**: Production cookies accessible via JavaScript, enabling XSS token theft
**Resolution**: Implemented secure cookie settings:
- `httpOnly(true)` to prevent XSS access
- `secure(!isLocalDev)` to require HTTPS in production
- `sameSite("Strict")` for CSRF protection in production

### HIGH SEVERITY VULNERABILITIES (PENDING)

#### 4. JWT Secret Key Management (HIGH RISK CONFIRMED ⚠️)
**Location**: Multiple files using `@Value("${custom.project.jwt.key}")`
**Status**: ENVIRONMENT VARIABLE DEPENDENCY - NEEDS IMPROVEMENT
**Current Implementation**: Uses `${JWT_KEY}` environment variable in prod, hardcoded in tests
**Risk Assessment**:
- ✅ Not hardcoded in source (uses env var in production)
- ❌ No key rotation mechanism
- ❌ Single point of failure if compromised
- ❌ Test keys are visible in source code

**Recommended Implementation**:
```kotlin
@Service
class JwtKeyRotationService {
    @Value("#{systemEnvironment['JWT_SIGNING_KEY'] ?: '${custom.project.jwt.fallback-key:}'}")
    private lateinit var primaryKey: String

    @Value("#{systemEnvironment['JWT_PREVIOUS_KEY'] ?: ''}")
    private lateinit var previousKey: String

    fun getCurrentSigningKey(): SecretKey = Keys.hmacShaKeyFor(primaryKey.toByteArray())

    fun getValidationKeys(): List<SecretKey> = listOfNotNull(
        Keys.hmacShaKeyFor(primaryKey.toByteArray()),
        if (previousKey.isNotBlank()) Keys.hmacShaKeyFor(previousKey.toByteArray()) else null
    )
}
```

#### 5. Username Enumeration via Registration (ACTIVE VULNERABILITY CONFIRMED 🚨)
**Location**: `LoginController.kt:151-153`
**Status**: VULNERABILITY CONFIRMED - IMMEDIATE FIX REQUIRED
**Impact**: Registration endpoint reveals username existence, enabling account enumeration
**Current Implementation**:
```kotlin
} catch (e: IllegalArgumentException) {
    logger.warn("Registration failed - username already exists: ${newUser.username}")
    return ResponseEntity.status(HttpStatus.CONFLICT).body(mapOf("error" to "Username already exists"))
}
```
**Attack Vector**: Automated scripts can test millions of usernames to build target lists
**Risk Level**: MEDIUM-HIGH - Enables targeted phishing, social engineering attacks

#### 6. Session Logout Implementation (RESOLVED ✅)
**Location**: `LoginController.kt:102-125`
**Status**: IMPLEMENTED
**Resolution**: Logout endpoint implemented that properly clears JWT cookie:
- Cookie cleared with empty value and maxAge=0
- Maintains secure cookie settings (httpOnly, secure, sameSite)
- Environment-aware configuration for local vs production

### MEDIUM SEVERITY VULNERABILITIES (PENDING)

#### 7. Insufficient Session Management
**Location**: JWT token handling throughout
**Issues**:
- ~~No logout mechanism~~ (RESOLVED ✅ - logout endpoint implemented)
- No token blacklisting on logout (cookie cleared but tokens remain valid until expiration)
- 1-hour expiration appropriate for current use case
- No concurrent session limits

**Recommended Implementation**:
```kotlin
@Service
class TokenBlacklistService {
    private val blacklistedTokens = ConcurrentHashMap<String, Long>()

    fun blacklistToken(token: String, expirationTime: Long) {
        blacklistedTokens[token] = expirationTime
        // Schedule cleanup for expired tokens
        scheduleCleanup(expirationTime)
    }

    fun isBlacklisted(token: String): Boolean = blacklistedTokens.containsKey(token)

    private fun scheduleCleanup(expirationTime: Long) {
        // Implementation for cleaning up expired blacklisted tokens
    }
}
```

#### 8. Rate Limiting Bypass Potential (CRITICAL VULNERABILITY CONFIRMED 🚨)
**Location**: `RateLimitingFilter.kt:99-107`
**Status**: ACTIVE VULNERABILITY - IMMEDIATE FIX REQUIRED
**Impact**: Attackers can easily bypass rate limiting by spoofing headers
**Current Implementation**: Filter blindly trusts proxy headers without validation:
```kotlin
private fun getClientIpAddress(request: HttpServletRequest): String {
    val xForwardedFor = request.getHeader("X-Forwarded-For")
    val xRealIp = request.getHeader("X-Real-IP")
    return when {
        !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
        !xRealIp.isNullOrBlank() -> xRealIp
        else -> request.remoteAddr ?: "unknown"
    }
}
```
**Vulnerability**: Any client can send `X-Forwarded-For: 192.168.1.1` to appear as different IP
**Risk Level**: HIGH - Enables unlimited API abuse, credential brute force attacks
```

**Recommended Implementation**:
```kotlin
private fun getClientIpAddress(request: HttpServletRequest): String {
    // Define trusted proxy networks
    val trustedProxies = listOf("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16")
    val clientIp = request.remoteAddr ?: "unknown"

    return if (isFromTrustedProxy(clientIp, trustedProxies)) {
        // Only trust proxy headers from known proxies
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")
        when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIp.isNullOrBlank() -> xRealIp
            else -> clientIp
        }
    } else {
        clientIp
    }
}

private fun isFromTrustedProxy(ip: String, trustedNetworks: List<String>): Boolean {
    // Implementation to check if IP is from trusted proxy network
    return trustedNetworks.any { network -> isIpInNetwork(ip, network) }
}
```

### ACTIVE ATTACK VECTORS (2025-09-14 Assessment)

#### 🚨 CRITICAL ACTIVE THREATS
1. **Rate Limit Bypass**: Header spoofing enables unlimited API abuse
2. **Username Enumeration**: Registration endpoint reveals account existence
3. **Token Persistence**: Logout doesn't invalidate JWT tokens

#### ⚠️ HIGH RISK VECTORS
4. **Brute Force Amplification**: No account lockout + rate limit bypass = unlimited attempts
5. **Session Extension**: Tokens valid for full duration even after "logout"
6. **Key Compromise Impact**: No rotation means single key compromise = total breach

#### ✅ MITIGATED THREATS
- ~~Credential Harvesting~~ (Password logging eliminated)
- ~~Timing Attacks~~ (Constant-time authentication implemented)
- ~~Session Hijacking~~ (Secure cookie configuration implemented)
- ~~XSS Token Theft~~ (HttpOnly cookies prevent JavaScript access)

## STRATEGIC IMPROVEMENTS (PRIORITY 2)

### 9. Account Lockout Mechanism
```kotlin
@Service
class LoginAttemptService {
    private val attemptCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val lockoutTimes = ConcurrentHashMap<String, Long>()
    private val maxAttempts = 5
    private val lockoutDurationMs = 900000L // 15 minutes

    fun recordFailedAttempt(username: String) {
        val attempts = attemptCounts.computeIfAbsent(username) { AtomicInteger(0) }
        if (attempts.incrementAndGet() >= maxAttempts) {
            lockoutTimes[username] = System.currentTimeMillis() + lockoutDurationMs
            logger.warn("Account locked due to excessive failed attempts: $username")
        }
    }

    fun isLocked(username: String): Boolean {
        val lockoutTime = lockoutTimes[username] ?: return false
        if (System.currentTimeMillis() >= lockoutTime) {
            // Lockout expired, clean up
            lockoutTimes.remove(username)
            attemptCounts.remove(username)
            return false
        }
        return true
    }

    fun recordSuccessfulAttempt(username: String) {
        attemptCounts.remove(username)
        lockoutTimes.remove(username)
    }
}
```

### 10. Enhanced Security Logging
```kotlin
@Component
class SecurityEventLogger {
    private val securityLogger = LoggerFactory.getLogger("SECURITY.Events")

    fun logAuthenticationSuccess(username: String, ipAddress: String, userAgent: String) {
        securityLogger.info("AUTH_SUCCESS: user={}, ip={}, agent={}",
            username, ipAddress, sanitizeUserAgent(userAgent))
    }

    fun logAuthenticationFailure(username: String?, ipAddress: String, reason: String) {
        securityLogger.warn("AUTH_FAILURE: user={}, ip={}, reason={}",
            username ?: "unknown", ipAddress, reason)
    }

    fun logSuspiciousActivity(event: String, details: Map<String, Any>) {
        securityLogger.error("SUSPICIOUS_ACTIVITY: event={}, details={}", event, details)
    }

    private fun sanitizeUserAgent(userAgent: String): String =
        userAgent.take(200).filter { it.isLetterOrDigit() || it in " .,;:-_()" }
}
```

### 11. Comprehensive Input Validation
```kotlin
@Component
class LoginInputValidator {

    fun validateLoginRequest(loginRequest: User): ValidationResult {
        val errors = mutableListOf<String>()

        // Username validation
        if (loginRequest.username.isBlank()) {
            errors.add("Username is required")
        } else if (!isValidUsernameFormat(loginRequest.username)) {
            errors.add("Invalid username format")
        }

        // Password validation
        if (loginRequest.password.isBlank()) {
            errors.add("Password is required")
        } else if (loginRequest.password.length > 1000) { // Prevent DoS
            errors.add("Password too long")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    private fun isValidUsernameFormat(username: String): Boolean {
        return username.matches("^[a-zA-Z0-9._@+-]{3,60}$".toRegex())
    }

    data class ValidationResult(val isValid: Boolean, val errors: List<String>)
}
```

## ADVANCED HARDENING (PRIORITY 3)

### 12. Multi-Factor Authentication Framework
```kotlin
@Service
class MfaService {
    fun generateTotpSecret(username: String): String {
        // Generate TOTP secret for user
        // Store securely in database
        return generateSecretKey()
    }

    fun verifyTotpCode(username: String, code: String): Boolean {
        // Verify TOTP code against stored secret
        val secret = getTotpSecret(username)
        return verifyTotp(secret, code)
    }

    fun isMfaRequired(username: String): Boolean {
        // Determine if MFA is required for this user
        return getUserMfaSettings(username).isEnabled
    }
}
```

### 13. Security Headers Configuration
```kotlin
@Configuration
class SecurityHeadersConfig {

    @Bean
    fun securityHeadersFilter(): FilterRegistrationBean<SecurityHeadersFilter> {
        val registrationBean = FilterRegistrationBean<SecurityHeadersFilter>()
        registrationBean.filter = SecurityHeadersFilter()
        registrationBean.addUrlPatterns("/*")
        registrationBean.order = 1
        return registrationBean
    }
}

@Component
class SecurityHeadersFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Security headers
        response.setHeader("X-Content-Type-Options", "nosniff")
        response.setHeader("X-Frame-Options", "DENY")
        response.setHeader("X-XSS-Protection", "1; mode=block")
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin")
        response.setHeader("Content-Security-Policy",
            "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'")

        filterChain.doFilter(request, response)
    }
}
```

## MONITORING RECOMMENDATIONS

### Security Event Monitoring
```yaml
# application-prod.yml additions
security:
  monitoring:
    failed-login-threshold: 10 # Alert after 10 failed logins per minute
    suspicious-ip-threshold: 50 # Alert for >50 requests/minute from single IP
    jwt-manipulation-detection: true
    geographic-anomaly-detection: true
```

### Metrics Collection
- **Authentication success/failure rates**
- **Response times for login attempts**
- **Rate limit violations**
- **Geographic access patterns**
- **Token manipulation attempts**

### Alert Thresholds
- **>10 failed attempts/minute**: Potential brute force attack
- **Unusual geographic access**: Account compromise indicator
- **Token manipulation attempts**: Active attack in progress
- **Rate limit violations**: Automated attack detection

## COMPLIANCE CONSIDERATIONS

### PCI-DSS Requirements
- ~~Remove password logging~~ (RESOLVED ✅)
- Implement proper key management (PENDING)
- Regular security assessments (ONGOING)

### GDPR Requirements
- Data breach notification procedures for credential exposure
- User consent for data processing
- Right to erasure implementation

### SOC 2 Requirements
- Access controls and authentication
- System monitoring and logging
- Incident response procedures
- Security awareness training

## IMPLEMENTATION PRIORITY

### CRITICAL (Immediate Action Required) 🚨
1. **Rate Limiting Header Spoofing Fix** - RateLimitingFilter.kt:99-107 confirmed vulnerable
2. **Username Enumeration in Registration** - LoginController.kt:151-153 still reveals existence
3. **JWT Token Blacklisting** - Tokens remain valid after logout until expiration

### HIGH PRIORITY (Next Sprint)
4. **JWT Key Rotation Service** - Currently uses single environment variable
5. **Account Lockout Mechanism** - No protection against brute force attacks
6. **Security Headers Implementation** - Missing comprehensive security headers
7. **Enhanced Input Validation** - Strengthen validation across all endpoints

### MEDIUM PRIORITY (Future Releases)
8. **Multi-factor Authentication Framework**
9. **Advanced Monitoring & Alerting**
10. **Behavioral Analytics & Anomaly Detection**
11. **Hardware Security Module (HSM) Integration**

### ✅ COMPLETED ITEMS
- ~~Remove password logging~~ (VERIFIED: No logging in UserService.kt)
- ~~Fix cookie security settings~~ (VERIFIED: Secure, httpOnly, sameSite implemented)
- ~~Implement constant-time authentication~~ (VERIFIED: Dummy hash for timing consistency)
- ~~Session logout implementation~~ (VERIFIED: Proper cookie clearing in place)

---

**Report Updated**: 2025-09-14
**Assessment Scope**: Comprehensive security analysis of authentication and authorization
**Risk Assessment**: Live code analysis with confirmed vulnerability testing
**Overall Security Status**: 🟡 MODERATE RISK - Critical vulnerabilities identified requiring immediate attention

**Verified Implementation Status**:
- ✅ Password security: No logging, proper hashing, constant-time comparison
- ✅ Cookie security: HttpOnly, Secure, SameSite configuration verified
- ✅ Logout mechanism: Proper cookie clearing implementation confirmed
- 🚨 Rate limiting: CONFIRMED vulnerable to X-Forwarded-For spoofing
- 🚨 Registration endpoint: CONFIRMED username enumeration vulnerability
- ⚠️ JWT management: Environment-based key, no rotation, no blacklisting

**Immediate Action Items**:
1. Fix rate limiting header validation (RateLimitingFilter.kt:99-107)
2. Implement generic registration error messages (LoginController.kt:151-153)
3. Add JWT token blacklisting for proper logout functionality

## UPDATED SECURITY IMPLEMENTATION ROADMAP

### Phase 1: CRITICAL FIXES (Deploy This Week)

#### 1.1 Fix Rate Limiting Header Spoofing
**File**: `src/main/kotlin/finance/configurations/RateLimitingFilter.kt:99-107`
**Issue**: Blindly trusts proxy headers allowing easy bypass
**Fix**: Implement trusted proxy validation
```kotlin
private fun getClientIpAddress(request: HttpServletRequest): String {
    val trustedProxies = listOf("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16")
    val clientIp = request.remoteAddr ?: "unknown"

    return if (isFromTrustedProxy(clientIp, trustedProxies)) {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        val xRealIp = request.getHeader("X-Real-IP")
        when {
            !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
            !xRealIp.isNullOrBlank() -> xRealIp
            else -> clientIp
        }
    } else {
        clientIp // Only use direct connection IP for untrusted sources
    }
}
```

#### 1.2 Eliminate Username Enumeration in Registration
**File**: `src/main/kotlin/finance/controllers/LoginController.kt:151-153`
**Issue**: Returns "Username already exists" revealing account existence
**Fix**: Generic error responses for all registration failures
```kotlin
} catch (e: IllegalArgumentException) {
    logger.warn("Registration failed for username: ${newUser.username} - ${e.message}")
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(mapOf("error" to "Registration failed. Please check your information and try again."))
} catch (e: Exception) {
    logger.error("Registration error for username: ${newUser.username}")
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(mapOf("error" to "Registration failed. Please check your information and try again."))
}
```

#### 1.3 Implement JWT Token Blacklisting
**New File**: `src/main/kotlin/finance/services/TokenBlacklistService.kt`
**Purpose**: Invalidate tokens on logout instead of just clearing cookies
**Implementation**:
```kotlin
@Service
class TokenBlacklistService {
    private val blacklistedTokens = ConcurrentHashMap<String, Long>()
    private val cleanup = Executors.newScheduledThreadPool(1)

    init {
        cleanup.scheduleWithFixedDelay(::cleanupExpiredTokens, 1, 1, TimeUnit.HOURS)
    }

    fun blacklistToken(token: String, expirationTime: Long) {
        blacklistedTokens[token] = expirationTime
        logger.info("Token blacklisted, expires at: ${Date(expirationTime)}")
    }

    fun isBlacklisted(token: String): Boolean = blacklistedTokens.containsKey(token)

    private fun cleanupExpiredTokens() {
        val now = System.currentTimeMillis()
        blacklistedTokens.entries.removeIf { it.value < now }
    }
}
```

### Phase 2: HIGH PRIORITY ENHANCEMENTS (Next Sprint)

#### 2.1 Account Lockout Implementation
**Files**: `LoginController.kt`, `UserService.kt`
**Purpose**: Prevent brute force attacks with progressive lockout
**Key Features**:
- 5 failed attempts = 15 minute lockout
- Progressive lockout duration for repeat offenses
- IP-based and username-based tracking

#### 2.2 JWT Key Rotation Service
**New File**: `src/main/kotlin/finance/services/JwtKeyRotationService.kt`
**Purpose**: Enable key rotation without service interruption
**Features**:
- Primary + previous key support for seamless rotation
- Environment variable + fallback configuration
- Automated rotation scheduling

#### 2.3 Security Headers Filter
**New File**: `src/main/kotlin/finance/configurations/SecurityHeadersFilter.kt`
**Purpose**: Add comprehensive security headers
**Headers**: X-Content-Type-Options, X-Frame-Options, CSP, HSTS

### Phase 3: MONITORING & ADVANCED SECURITY (Future)

#### 3.1 Enhanced Security Logging
- Structured security event logging
- Failed attempt correlation
- Geographic anomaly detection
- Suspicious pattern identification

#### 3.2 Advanced Rate Limiting
- Per-endpoint rate limits
- Adaptive rate limiting based on user behavior
- Distributed rate limiting for load balancing

#### 3.3 Multi-Factor Authentication
- TOTP implementation
- Backup codes
- Device registration and trust

### Testing Requirements for Each Phase

#### Phase 1 Testing (Critical Fixes)
```bash
# Test rate limiting bypass prevention
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "*RateLimiting*"

# Test registration error consistency
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "*Registration*"

# Test token blacklisting
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "*Logout*"
```

#### Security Test Coverage Requirements
- ✅ All critical fixes must have functional tests
- ✅ Penetration testing for rate limit bypass
- ✅ Username enumeration prevention validation
- ✅ Token invalidation verification
- ✅ Regression testing for all existing functionality

### Risk Assessment Matrix

| Vulnerability | Current Risk | Post-Fix Risk | Implementation Priority |
|---------------|-------------|---------------|----------------------|
| Rate Limit Bypass | 🔴 CRITICAL | 🟢 LOW | 1 (This Week) |
| Username Enumeration | 🟡 MEDIUM-HIGH | 🟢 LOW | 1 (This Week) |
| Token Persistence | 🟡 MEDIUM | 🟢 LOW | 1 (This Week) |
| No Account Lockout | 🟡 MEDIUM | 🟢 LOW | 2 (Next Sprint) |
| Key Rotation | 🟡 MEDIUM | 🟢 LOW | 2 (Next Sprint) |
| Missing Security Headers | 🟡 MEDIUM | 🟢 LOW | 2 (Next Sprint) |
