# Injection Attack Prevention Plan
**raspi-finance-endpoint Security Hardening**

**Date:** 2025-12-05
**Status:** Comprehensive security assessment and actionable remediation plan

---

## Executive Summary

Your application demonstrates **strong foundational security** with multiple defensive layers. However, several critical vulnerabilities require immediate attention:

### Current Security Status: 🟢 GOOD (with gaps)

| Attack Vector | Current Status | Priority |
|---------------|----------------|----------|
| SQL Injection | ✅ **PROTECTED** - All parameterized queries | None |
| Command Injection | ✅ **PROTECTED** - No process execution | None |
| Path Traversal | ✅ **PROTECTED** - No file upload handling | None |
| XSS | ✅ **PROTECTED** - No server-side templates | None |
| LDAP Injection | ✅ **PROTECTED** - No LDAP authentication | None |
| GraphQL DoS | ⚠️ **MITIGATED** - Has depth/complexity limits | Low |
| **IP Spoofing** | 🔴 **VULNERABLE** - Trusts proxy headers | **CRITICAL** |
| Authorization | ⚠️ **INCONSISTENT** - No method-level checks on REST | **HIGH** |
| Input Validation | ⚠️ **MINOR GAPS** - UUID pattern, password validation | Medium |

---

## 🔴 CRITICAL Priority Fixes

### 1. IP Spoofing Vulnerability (CRITICAL)

**Affected Files:**
- `src/main/kotlin/finance/configurations/SecurityAuditFilter.kt:178-187`
- `src/main/kotlin/finance/controllers/BaseController.kt:144-155`
- `src/main/kotlin/finance/configurations/JwtAuthenticationFilter.kt:164-172`

**Vulnerability:** Three components trust `X-Forwarded-For` and `X-Real-IP` headers without validation, allowing attackers to:
- Forge IP addresses in security logs
- Potentially bypass IP-based security controls
- Impersonate other users in audit trails

**Current Vulnerable Code Pattern:**
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

**Attack Scenario:**
```bash
# Attacker can forge their IP address
curl -H "X-Forwarded-For: 127.0.0.1" https://api.bhenning.com/api/payment
# Your logs will show 127.0.0.1 instead of attacker's real IP
```

**Solution:** Apply the proven pattern from `RateLimitingFilter.kt:119-146`

**Remediation Steps:**

#### Step 1: Create Shared IP Validation Utility

Create: `src/main/kotlin/finance/utils/IpAddressValidator.kt`

```kotlin
package finance.utils

import org.slf4j.LoggerFactory
import jakarta.servlet.http.HttpServletRequest

object IpAddressValidator {
    private val logger = LoggerFactory.getLogger(IpAddressValidator::class.java)

    /**
     * Safely extracts client IP address with proxy header validation.
     * Only trusts X-Forwarded-For/X-Real-IP from known private networks.
     */
    fun getClientIpAddress(request: HttpServletRequest): String {
        val clientIp = request.remoteAddr ?: "unknown"

        // Only trust proxy headers from known trusted networks
        return if (isFromTrustedProxy(clientIp)) {
            val xForwardedFor = request.getHeader("X-Forwarded-For")
            val xRealIp = request.getHeader("X-Real-IP")
            when {
                !xForwardedFor.isNullOrBlank() -> {
                    val forwardedIp = xForwardedFor.split(",")[0].trim()
                    if (isValidIpAddress(forwardedIp)) forwardedIp else clientIp
                }
                !xRealIp.isNullOrBlank() -> {
                    if (isValidIpAddress(xRealIp)) xRealIp else clientIp
                }
                else -> clientIp
            }
        } else {
            // For untrusted sources, ignore proxy headers
            logger.debug("Ignoring proxy headers from untrusted IP: {}", clientIp)
            clientIp
        }
    }

    private fun isFromTrustedProxy(clientIp: String): Boolean {
        if (clientIp == "unknown") return false

        val trustedNetworks = listOf(
            "10.0.0.0/8",      // Private Class A
            "172.16.0.0/12",   // Private Class B
            "192.168.0.0/16",  // Private Class C
            "127.0.0.0/8",     // Loopback
        )

        return trustedNetworks.any { isIpInNetwork(clientIp, it) }
    }

    private fun isValidIpAddress(ip: String): Boolean {
        // IPv4 validation
        val ipv4Regex = """^(\d{1,3}\.){3}\d{1,3}$""".toRegex()
        if (ipv4Regex.matches(ip)) {
            val parts = ip.split(".")
            return parts.all { it.toIntOrNull()?.let { num -> num in 0..255 } ?: false }
        }

        // IPv6 validation (simplified)
        val ipv6Regex = """^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$""".toRegex()
        return ipv6Regex.matches(ip)
    }

    private fun isIpInNetwork(ip: String, cidr: String): Boolean {
        try {
            val parts = cidr.split("/")
            val networkAddress = parts[0]
            val prefixLength = parts[1].toInt()

            val ipBytes = ipToBytes(ip)
            val networkBytes = ipToBytes(networkAddress)

            if (ipBytes.size != networkBytes.size) return false

            val fullBytes = prefixLength / 8
            val remainingBits = prefixLength % 8

            // Check full bytes
            for (i in 0 until fullBytes) {
                if (ipBytes[i] != networkBytes[i]) return false
            }

            // Check remaining bits
            if (remainingBits > 0 && fullBytes < ipBytes.size) {
                val mask = (0xFF shl (8 - remainingBits)) and 0xFF
                if ((ipBytes[fullBytes].toInt() and mask) != (networkBytes[fullBytes].toInt() and mask)) {
                    return false
                }
            }

            return true
        } catch (e: Exception) {
            logger.warn("Failed to parse IP/CIDR: ip={}, cidr={}", ip, cidr, e)
            return false
        }
    }

    private fun ipToBytes(ip: String): ByteArray {
        return ip.split(".").map { it.toInt().toByte() }.toByteArray()
    }
}
```

#### Step 2: Update SecurityAuditFilter

**File:** `src/main/kotlin/finance/configurations/SecurityAuditFilter.kt`

**Change lines 178-187 from:**
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

**To:**
```kotlin
import finance.utils.IpAddressValidator

// Remove getClientIpAddress() method entirely
// Replace all calls to getClientIpAddress(request) with:
// IpAddressValidator.getClientIpAddress(request)
```

#### Step 3: Update BaseController

**File:** `src/main/kotlin/finance/controllers/BaseController.kt`

**Change lines 144-155 from:**
```kotlin
private fun getClientIpAddress(request: HttpServletRequest?): String {
    if (request == null) return "unknown"

    val xForwardedFor = request.getHeader("X-Forwarded-For")
    val xRealIp = request.getHeader("X-Real-IP")

    return when {
        !xForwardedFor.isNullOrBlank() -> xForwardedFor.split(",")[0].trim()
        !xRealIp.isNullOrBlank() -> xRealIp
        else -> request.remoteAddr ?: "unknown"
    }
}
```

**To:**
```kotlin
import finance.utils.IpAddressValidator

private fun getClientIpAddress(request: HttpServletRequest?): String {
    if (request == null) return "unknown"
    return IpAddressValidator.getClientIpAddress(request)
}
```

#### Step 4: Update JwtAuthenticationFilter

**File:** `src/main/kotlin/finance/configurations/JwtAuthenticationFilter.kt`

**Same pattern as SecurityAuditFilter** - replace vulnerable method with call to `IpAddressValidator.getClientIpAddress(request)`

#### Step 5: Update RateLimitingFilter (For Consistency)

**File:** `src/main/kotlin/finance/configurations/RateLimitingFilter.kt`

**Replace lines 119-146** with call to shared utility:
```kotlin
import finance.utils.IpAddressValidator

private fun getClientIpAddress(request: HttpServletRequest): String {
    return IpAddressValidator.getClientIpAddress(request)
}

// Remove isFromTrustedProxy, isValidIpAddress, isIpInNetwork, ipToBytes methods
```

#### Step 6: Add Unit Tests

Create: `src/test/unit/groovy/finance/utils/IpAddressValidatorSpec.groovy`

```groovy
package finance.utils

import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification

class IpAddressValidatorSpec extends Specification {

    def "should trust X-Forwarded-For from private network proxy"() {
        given:
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"  // Private network
        request.getHeader("X-Forwarded-For") >> "203.0.113.45"

        when:
        def result = IpAddressValidator.getClientIpAddress(request)

        then:
        result == "203.0.113.45"
    }

    def "should ignore X-Forwarded-For from public IP"() {
        given:
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "203.0.113.50"  // Public IP
        request.getHeader("X-Forwarded-For") >> "10.0.0.1"

        when:
        def result = IpAddressValidator.getClientIpAddress(request)

        then:
        result == "203.0.113.50"  // Should use remoteAddr, not header
    }

    def "should validate IPv4 format"() {
        given:
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"
        request.getHeader("X-Forwarded-For") >> invalidIp

        when:
        def result = IpAddressValidator.getClientIpAddress(request)

        then:
        result == "192.168.1.1"  // Falls back to remoteAddr

        where:
        invalidIp << ["999.999.999.999", "not.an.ip.address", "'; DROP TABLE users;--"]
    }

    def "should handle missing headers gracefully"() {
        given:
        def request = Mock(HttpServletRequest)
        request.remoteAddr >> "192.168.1.1"
        request.getHeader("X-Forwarded-For") >> null
        request.getHeader("X-Real-IP") >> null

        when:
        def result = IpAddressValidator.getClientIpAddress(request)

        then:
        result == "192.168.1.1"
    }
}
```

**Test Command:**
```bash
./gradlew test --tests "finance.utils.IpAddressValidatorSpec"
```

**Verification:**
1. Run tests: `./gradlew test`
2. Check logs for "Ignoring proxy headers from untrusted IP" warnings
3. Review security audit logs to ensure IP addresses are now trustworthy

**Impact:** Prevents attackers from forging IP addresses in security-critical operations.

---

## 🟠 HIGH Priority Fixes

### 2. REST API Method-Level Authorization (HIGH)

**Vulnerability:** REST controllers lack `@PreAuthorize` annotations, relying only on URL pattern-based security.

**Affected Files:**
- `src/main/kotlin/finance/controllers/AccountController.kt`
- `src/main/kotlin/finance/controllers/TransactionController.kt`
- `src/main/kotlin/finance/controllers/CategoryController.kt`
- `src/main/kotlin/finance/controllers/DescriptionController.kt`
- `src/main/kotlin/finance/controllers/ParameterController.kt`
- `src/main/kotlin/finance/controllers/PaymentController.kt`
- `src/main/kotlin/finance/controllers/MedicalExpenseController.kt`
- `src/main/kotlin/finance/controllers/ReceiptImageController.kt`

**Risk:** If URL pattern configuration is accidentally misconfigured, endpoints could become exposed.

**Current State:**
```kotlin
@RestController
@RequestMapping("/account")
class AccountController {
    @GetMapping("/select/active")
    fun getActiveAccounts(): ResponseEntity<*> { ... }
}
```

**Recommended Pattern:**
```kotlin
@RestController
@RequestMapping("/account")
class AccountController {
    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/select/active")
    fun getActiveAccounts(): ResponseEntity<*> { ... }
}
```

**Remediation Steps:**

#### Step 1: Add Class-Level Authorization (Apply to ALL REST Controllers)

**Example for AccountController:**
```kotlin
package finance.controllers

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/account")
@PreAuthorize("hasAuthority('USER')")  // <-- Add this line
class AccountController(
    private val accountService: AccountService,
) : BaseController() {
    // All methods now require USER authority
}
```

**Apply to these controllers:**
- AccountController
- TransactionController
- CategoryController
- DescriptionController
- ParameterController
- PaymentController
- ValidationAmountController
- MedicalExpenseController
- ReceiptImageController

#### Step 2: Exclude Public Endpoints (LoginController)

**LoginController** should **NOT** have `@PreAuthorize` at class level, since `/api/login` and `/api/register` must be public.

**Current Implementation (Correct):**
```kotlin
@RestController
@RequestMapping("/api")
class LoginController {
    @PostMapping("/login")
    fun login(...) // Public

    @PostMapping("/register")
    fun register(...) // Public

    @PreAuthorize("hasAuthority('USER')")
    @PostMapping("/logout")
    fun logout(...) // Protected
}
```

#### Step 3: Document Authorization Requirements

Create: `docs/AUTHORIZATION_MATRIX.md`

```markdown
# Authorization Matrix

## REST API Endpoints

| Endpoint Pattern | Required Authority | Notes |
|-----------------|-------------------|-------|
| `/api/login` | Public | Authentication endpoint |
| `/api/register` | Public | User registration |
| `/api/logout` | USER | Requires authentication |
| `/account/**` | USER | All account operations |
| `/transaction/**` | USER | All transaction operations |
| `/category/**` | USER | All category operations |
| `/description/**` | USER | All description operations |
| `/payment/**` | USER | All payment operations |
| `/medical-expense/**` | USER | All medical expense operations |
| `/parameter/**` | USER | All parameter operations |
| `/receipt-image/**` | USER | All receipt operations |
| `/graphql` | USER | GraphQL endpoint (queries & mutations) |
| `/graphiql` | USER | GraphQL IDE |

## Future Enhancements

Consider implementing role-based access control (RBAC):
- `ROLE_USER` - Standard user operations
- `ROLE_ADMIN` - Administrative operations (bulk delete, system config)
- `ROLE_READ_ONLY` - View-only access for reports/audits
```

#### Step 4: Add Integration Test

Create: `src/test/functional/groovy/finance/security/AuthorizationSecurityFunctionalSpec.groovy`

```groovy
package finance.security

import finance.helpers.TestFixtures
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import spock.lang.Specification

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthorizationSecurityFunctionalSpec extends Specification {

    @Autowired
    TestRestTemplate restTemplate

    def "should deny access to protected endpoints without JWT"() {
        when: "accessing protected endpoint without authentication"
        def response = restTemplate.getForEntity("/account/select/active", String)

        then: "should return 401 Unauthorized or 403 Forbidden"
        response.statusCode in [HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN]
    }

    def "should allow access to public endpoints"() {
        when: "accessing public login endpoint"
        def response = restTemplate.postForEntity(
            "/api/login",
            [username: "test", password: "invalid"],
            String
        )

        then: "should not return authentication error (may return 401 for bad credentials)"
        response != null
    }
}
```

**Test Command:**
```bash
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.security.AuthorizationSecurityFunctionalSpec"
```

**Verification:**
1. Apply `@PreAuthorize("hasAuthority('USER')")` to all controllers
2. Run functional tests
3. Verify no regression in existing functionality
4. Test with Postman/curl without JWT - should receive 401/403

**Impact:** Defense-in-depth - even if URL pattern security is misconfigured, method-level security provides a safety net.

---

## 🟡 MEDIUM Priority Fixes

### 3. Password Validation Bypass (MEDIUM)

**Affected File:** `src/main/kotlin/finance/controllers/LoginController.kt:354-362`

**Vulnerability:** Registration allows pre-encoded BCrypt passwords to bypass validation.

**Current Code:**
```kotlin
private fun isValidRawPassword(password: String): Boolean {
    if (password.startsWith("$2a$") || password.startsWith("$2b$")) {
        return true  // BYPASS: Allows pre-encoded passwords
    }
    val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$"
    return password.length >= 8 && password.matches(passwordRegex.toRegex())
}
```

**Attack Scenario:**
```bash
# Attacker can submit weak password as BCrypt hash
curl -X POST https://api.bhenning.com/api/register \
  -d '{"username":"attacker","password":"$2a$10$abcdefghijklmnopqrstuv"}' \
  -H "Content-Type: application/json"

# Bypass bypasses complexity requirements
```

**Remediation:**

**Change lines 354-362 to:**
```kotlin
private fun isValidRawPassword(password: String): Boolean {
    // Never accept pre-encoded passwords from user input
    if (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$")) {
        logger.warn("SECURITY: Rejected pre-encoded password in registration attempt")
        return false  // REJECT: Pre-encoded passwords are invalid input
    }

    val passwordRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$"
    return password.length >= 8 && password.matches(passwordRegex.toRegex())
}
```

**Add Unit Test:**

Update: `src/test/unit/groovy/finance/controllers/LoginControllerSpec.groovy`

```groovy
def "should reject pre-encoded BCrypt passwords in registration"() {
    when: "attempting to register with BCrypt hash as password"
    def response = loginController.register(
        "newuser",
        "\$2a\$10\$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"  // weak password hash
    )

    then: "should reject the registration"
    response.statusCode == HttpStatus.BAD_REQUEST
    response.body.message.contains("password")
}
```

**Impact:** Prevents attackers from bypassing password complexity requirements.

---

### 4. UUID Pattern Validation Gap (MEDIUM)

**Affected File:** `src/main/kotlin/finance/utils/Constants.kt`

**Issue:** UUID pattern only accepts lowercase hex digits, rejecting valid uppercase UUIDs.

**Current Pattern:**
```kotlin
const val UUID_PATTERN = "^[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}$"
```

**Problem:** Rejects valid UUIDs like `550E8400-E29B-41D4-A716-446655440000`

**Remediation:**

**Change to:**
```kotlin
const val UUID_PATTERN = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"
```

**Add Test Case:**

Update: `src/test/unit/groovy/finance/controllers/dto/*InputDtoSpec.groovy`

```groovy
def "should accept uppercase UUID"() {
    when:
    def dto = new PaymentInputDto(
        "550E8400-E29B-41D4-A716-446655440000",  // Uppercase UUID
        "checking_primary",
        "bills_payable",
        Date.valueOf("2024-01-15"),
        new BigDecimal("100.00"),
        null, null, null
    )

    then:
    validator.validate(dto).isEmpty()
}
```

**Impact:** Aligns validation with UUID RFC 4122 standard.

---

### 5. GraphQL Exception Information Disclosure (MEDIUM)

**Affected File:** `src/main/kotlin/finance/controllers/graphql/GraphQLExceptionHandler.kt`

**Issue:** Constraint violations return detailed messages that could aid reconnaissance.

**Current Code:**
```kotlin
@ExceptionHandler(ConstraintViolationException::class)
fun handleConstraintViolation(ex: ConstraintViolationException): GraphQLError {
    val violations = ex.constraintViolations.joinToString("; ") { "${it.propertyPath}: ${it.message}" }
    return GraphqlErrorBuilder.newError()
        .message("Validation failed: $violations")
        .errorType(ErrorType.ValidationError)
        .build()
}
```

**Information Disclosed:**
- Field names
- Validation constraints
- Data structure

**Remediation:**

**Change to:**
```kotlin
@ExceptionHandler(ConstraintViolationException::class)
fun handleConstraintViolation(ex: ConstraintViolationException): GraphQLError {
    // Log full details server-side for debugging
    logger.warn("GraphQL validation error: {}", ex.constraintViolations.joinToString("; ") {
        "${it.propertyPath}: ${it.message}"
    })

    // Return generic message to client
    return GraphqlErrorBuilder.newError()
        .message("Validation failed. Please check your input and try again.")
        .errorType(ErrorType.ValidationError)
        .build()
}
```

**Configuration Option (Alternative):**

Add environment-specific behavior:

```kotlin
private val isProduction = System.getenv("SPRING_PROFILES_ACTIVE")?.contains("prod") == true

@ExceptionHandler(ConstraintViolationException::class)
fun handleConstraintViolation(ex: ConstraintViolationException): GraphQLError {
    val violations = ex.constraintViolations.joinToString("; ") { "${it.propertyPath}: ${it.message}" }

    // Log full details
    logger.warn("GraphQL validation error: {}", violations)

    // Return detailed errors in non-production, generic in production
    val message = if (isProduction) {
        "Validation failed. Please check your input."
    } else {
        "Validation failed: $violations"
    }

    return GraphqlErrorBuilder.newError()
        .message(message)
        .errorType(ErrorType.ValidationError)
        .build()
}
```

**Impact:** Reduces information leakage to potential attackers while maintaining debuggability.

---

## 🟢 LOW Priority Enhancements

### 6. Rate Limiting Improvements (LOW)

**Current Rate Limit:** 5000 requests/minute (83 req/sec)

**Recommendations:**

1. **Lower Default Rate Limit for Financial Operations**

Update `application-prod.yml`:
```yaml
custom:
  security:
    rate-limit:
      enabled: true
      requests-per-minute: ${RATE_LIMIT_RPM:1000}  # Lower from 5000 to 1000
      window-size-minutes: 1
```

2. **Implement Per-User Rate Limiting**

Add user-specific rate limiting for authenticated requests:

```kotlin
// In RateLimitingFilter.kt
private fun getRateLimitKey(request: HttpServletRequest): String {
    val authentication = SecurityContextHolder.getContext().authentication
    return if (authentication?.isAuthenticated == true) {
        "user:${authentication.name}"  // Per-user rate limit
    } else {
        "ip:${getClientIpAddress(request)}"  // Per-IP for anonymous
    }
}
```

3. **Add Endpoint-Specific Rate Limits**

```kotlin
private val endpointLimits = mapOf(
    "/api/login" to 10,        // 10 login attempts per minute
    "/api/register" to 5,      // 5 registrations per minute
    "/payment/**" to 60,       // 60 payments per minute
    "default" to 1000          // 1000 for other endpoints
)
```

---

### 7. GraphQL Query Complexity Tuning (LOW)

**Current Limits:**
- Max query depth: 12
- Max complexity: 300

**Issue:** These limits may be too restrictive for legitimate nested queries.

**Recommendation:**

Monitor query metrics and adjust based on real usage:

```kotlin
// In GraphQLGuardrailsConfig.kt
@Bean
fun maxQueryDepthInstrumentation(): MaxQueryDepthInstrumentation {
    return MaxQueryDepthInstrumentation(
        maxDepth = System.getenv("GRAPHQL_MAX_DEPTH")?.toIntOrNull() ?: 15  // Increase to 15
    )
}

@Bean
fun maxQueryComplexityInstrumentation(): MaxQueryComplexityInstrumentation {
    return MaxQueryComplexityInstrumentation(
        maxComplexity = System.getenv("GRAPHQL_MAX_COMPLEXITY")?.toIntOrNull() ?: 500  // Increase to 500
    )
}
```

---

### 8. CORS Configuration Hardening (LOW)

**Current Issue:** Multiple hardcoded domain patterns

**File:** `src/main/kotlin/finance/configurations/WebSecurityConfig.kt`

**Recommendation:**

Move CORS origins to environment configuration:

```kotlin
// In WebSecurityConfig.kt
private val allowedOrigins: List<String> by lazy {
    System.getenv("CORS_ALLOWED_ORIGINS")
        ?.split(",")
        ?.map { it.trim() }
        ?: listOf(
            "http://localhost:3000",
            "https://bhenning.com",
            "https://brianhenning.com"
        )
}
```

**In env.secrets:**
```bash
export CORS_ALLOWED_ORIGINS="https://bhenning.com,https://brianhenning.com,http://localhost:3000"
```

---

## Additional Security Recommendations

### 9. Input Sanitization Headers

Already implemented in `HttpErrorLoggingFilter.kt` - excellent work!

**Current Sanitization:**
- Masks Authorization, Cookie, X-Api-Key headers
- Sanitizes User-Agent, Referer
- Replaces UUIDs/IDs in URLs with placeholders

**Enhancement:** Consider adding request body sanitization for sensitive fields:

```kotlin
private val sensitiveJsonFields = setOf("password", "ssn", "creditCard", "apiKey", "secret")

private fun sanitizeRequestBody(body: String): String {
    return sensitiveJsonFields.fold(body) { acc, field ->
        acc.replace(Regex(""""$field"\s*:\s*"[^"]*""""), """"$field":"***"""")
    }
}
```

---

### 10. SQL Injection Prevention - Already Excellent

**Current Status:** ✅ **FULLY PROTECTED**

All database access uses:
- JPA parameterized queries with `@Param` annotations
- No dynamic query construction
- No string concatenation in native queries

**Example of Safe Pattern:**
```kotlin
@Query("SELECT COUNT(t) FROM Transaction t WHERE t.description = :descriptionName")
fun countByDescriptionName(@Param("descriptionName") descriptionName: String): Long
```

**No changes needed** - your implementation is textbook secure.

---

### 11. Command Injection Prevention - Already Excellent

**Current Status:** ✅ **FULLY PROTECTED**

No usage of:
- `Runtime.getRuntime().exec()`
- `ProcessBuilder`
- Shell command execution

**Only shell script found:** `validate-docker-security.sh` (build tool, not runtime)

**No changes needed** - no command injection vectors exist.

---

### 12. Path Traversal Prevention - Already Excellent

**Current Status:** ✅ **FULLY PROTECTED**

No file upload handling with user-controlled paths detected.

`ReceiptImageController` uses JPA entity persistence, not file system operations.

**No changes needed** - no path traversal vectors exist.

---

## Implementation Roadmap

### Phase 1: CRITICAL (Complete within 1 week)
1. ✅ **Fix IP Spoofing Vulnerability**
   - Create `IpAddressValidator.kt` utility
   - Update SecurityAuditFilter, BaseController, JwtAuthenticationFilter
   - Add unit tests
   - Verify in logs

### Phase 2: HIGH (Complete within 2 weeks)
2. ✅ **Add Method-Level Authorization**
   - Apply `@PreAuthorize` to all REST controllers
   - Add authorization integration tests
   - Document authorization matrix

### Phase 3: MEDIUM (Complete within 1 month)
3. ✅ **Fix Password Validation Bypass**
   - Reject pre-encoded passwords
   - Add unit test

4. ✅ **Fix UUID Pattern Validation**
   - Update regex to accept uppercase
   - Add test cases

5. ✅ **Reduce GraphQL Exception Information Disclosure**
   - Return generic validation messages in production
   - Keep detailed logs server-side

### Phase 4: LOW (Ongoing improvements)
6. ⚠️ **Rate Limiting Enhancements**
   - Lower default rate limit
   - Implement per-user rate limiting
   - Add endpoint-specific limits

7. ⚠️ **GraphQL Complexity Tuning**
   - Monitor real usage
   - Adjust limits based on data

8. ⚠️ **CORS Configuration Hardening**
   - Move origins to environment variables

---

## Testing Strategy

### Unit Tests
```bash
./gradlew test --tests "finance.utils.IpAddressValidatorSpec"
./gradlew test --tests "finance.controllers.LoginControllerSpec"
./gradlew test --tests "finance.controllers.dto.*InputDtoSpec"
```

### Integration Tests
```bash
SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest
```

### Functional Tests
```bash
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.security.AuthorizationSecurityFunctionalSpec"
```

### Security Testing
```bash
# SQL Injection Testing
./gradlew test --tests "*RepositoryIntSpec"

# Authorization Testing
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.security.*"
```

---

## Monitoring and Alerting

### Key Metrics to Monitor

1. **IP Spoofing Detection**
   - Monitor logs for: `"Ignoring proxy headers from untrusted IP"`
   - Alert on sudden spikes in untrusted proxy header attempts

2. **Authentication Failures**
   - Monitor `security.audit.http.4xx` metrics
   - Alert on repeated 401/403 responses from same IP

3. **Rate Limiting**
   - Monitor `rate.limit.exceeded` counter
   - Alert on sustained rate limit violations

4. **GraphQL Complexity**
   - Monitor query depth/complexity rejections
   - Alert on patterns of complex query attacks

### Log Monitoring Queries

```bash
# IP spoofing attempts
grep "Ignoring proxy headers from untrusted IP" /var/log/raspi-finance-endpoint.log

# Authentication failures
grep "SECURITY_HTTP_4XX status=401" /var/log/raspi-finance-endpoint.log

# Pre-encoded password attempts
grep "Rejected pre-encoded password" /var/log/raspi-finance-endpoint.log

# Rate limit violations
grep "Rate limit exceeded" /var/log/raspi-finance-endpoint.log
```

---

## Security Checklist

### Completed ✅
- [x] SQL Injection protection (parameterized queries)
- [x] Command Injection protection (no process execution)
- [x] Path Traversal protection (no file upload)
- [x] XSS protection (no server-side templates)
- [x] CSRF protection (SameSite cookies, stateless JWT)
- [x] JWT authentication with HttpOnly cookies
- [x] Rate limiting with proper IP validation
- [x] Security headers (X-Frame-Options, CSP, HSTS)
- [x] Input validation with Jakarta annotations
- [x] Password hashing with BCrypt
- [x] GraphQL query depth/complexity limits
- [x] Sensitive header masking in logs
- [x] Comprehensive security audit logging

### To Implement 🔧
- [ ] IP spoofing protection in SecurityAuditFilter
- [ ] IP spoofing protection in BaseController
- [ ] IP spoofing protection in JwtAuthenticationFilter
- [ ] Method-level authorization on REST endpoints
- [ ] Password validation hardening (reject pre-encoded)
- [ ] UUID pattern fix (accept uppercase)
- [ ] GraphQL exception message sanitization
- [ ] Per-user rate limiting
- [ ] CORS configuration via environment variables
- [ ] Authorization integration tests

### Future Enhancements 📋
- [ ] Role-based access control (RBAC)
- [ ] Token refresh mechanism
- [ ] Request deduplication for transactions
- [ ] Idempotency key support
- [ ] Content-length limits
- [ ] Request body sanitization in logs
- [ ] Automated security scanning (OWASP Dependency Check)

---

## Compliance Considerations

### OWASP Top 10 (2021) Mapping

| OWASP Risk | Status | Mitigation |
|------------|--------|-----------|
| A01: Broken Access Control | ⚠️ Partial | Fix: Add method-level authorization |
| A02: Cryptographic Failures | ✅ Protected | BCrypt password hashing, HTTPS enforced |
| A03: Injection | ✅ Protected | Parameterized queries, input validation |
| A04: Insecure Design | ✅ Protected | Stateless JWT, rate limiting, circuit breakers |
| A05: Security Misconfiguration | ⚠️ Partial | Fix: IP spoofing, environment-based CORS |
| A06: Vulnerable Components | ⚠️ Unknown | Run: `./gradlew dependencyUpdates` |
| A07: Authentication Failures | ✅ Protected | JWT with proper expiration, password complexity |
| A08: Software Data Integrity | ✅ Protected | No dynamic code loading, signed JWTs |
| A09: Logging Failures | ✅ Protected | Comprehensive security audit logging |
| A10: SSRF | ✅ Protected | No external URL fetching based on user input |

---

## References

### Internal Documentation
- `/home/henninb/projects/github.com/henninb/raspi-finance-endpoint/CLAUDE.md` - Project guidelines
- `/home/henninb/projects/github.com/henninb/raspi-finance-endpoint/GRAPHQL.md` - GraphQL architecture

### Security Best Practices
- OWASP Top 10: https://owasp.org/Top10/
- OWASP Injection Prevention: https://cheatsheetseries.owasp.org/cheatsheets/Injection_Prevention_Cheat_Sheet.html
- Spring Security Reference: https://docs.spring.io/spring-security/reference/
- JWT Best Practices: https://tools.ietf.org/html/rfc8725

---

## Contact & Support

For security concerns or questions about this plan:
1. Review implementation with security team
2. Run automated security scans: `./validate-docker-security.sh`
3. Conduct penetration testing after Phase 1-2 completion
4. Monitor security audit logs continuously

**Last Updated:** 2025-12-05
**Review Frequency:** Quarterly security audits
**Next Review:** 2025-03-05
