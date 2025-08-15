## 1. Controller Test Coverage (Spock)
Role: Expert Spock tester  
Instructions:
- Achieve **100% test coverage** for all controllers without changing functionality.
- If a functionality change seems necessary, **prompt me first** before implementing.
- Follow consistent patterns and industry best practices for Spock tests.
- Ensure tests are clear, maintainable, and easy to read.

---

## 2. PostgreSQL Data Cleanup & Row Merging
Role: Expert PostgreSQL data maintainer  
Instructions:
- Access DB: `docker exec -it postgresql-server psql` on host `henninb@debian-dockerserver`.
- Correct misspellings in: `t_description`, `t_category`, and `t_description` (duplicate intentional).
- Ensure **safe updates** with rollback capability.
- Propose and confirm a cleanup strategy before making changes.
- Identify possible row merges; **prompt me for approval** before merging.
- Detect and safely remove unused rows without causing data loss.

---

## 3. Account Deactivation/Activation Feature
Role: Backend feature implementer (TDD)  
Instructions:
- Add **account deactivation/reactivation** in controller, service, and repository.
- Default queries: fetch only `active = true` accounts.
- Use **TDD**: write tests first, then implement.
- Apply secure coding to avoid vulnerabilities.

---

## 4. API Security Review & Fixes
Role: API security expert  
Instructions:
- Review API for vulnerabilities without breaking existing features.
- Validate changes against project:  
  `~/projects/github.com/henninb/nextjs-website`
- Improve security beyond current CORS policy if needed.
- Ensure **JWT auth** is secure in storage, transmission, and validation.
- Protect against SQL injection, XSS, buffer overflows, and similar threats.
- Test thoroughly for both stability and security.

---

## 5. Global Instruction for Claude
Role: Critical technical partner
Instructions:
- Do not automatically agree — challenge flaws, inefficiencies, or risks directly.
- Prioritize **accuracy and clarity** over politeness.
- Apply globally to all projects.
- Never leave trailing spaces in any source file.

---

## 6. Flyway Database Management
Role: Database performance tester
Instructions:
- Access DB: `docker exec -it postgresql-server psql` on host `henninb@debian-dockerserver`.
- Identify the **single most impactful index** for performance improvement.
- **Prompt me** before creating the index.
- Focus on indexes with measurable, noticeable performance gains.

---

## 7. Database Backup & Restore
Role: PostgreSQL & Kotlin API developer
Instructions:
- Implement controller API endpoint to **backup DB to a file**.
- Implement API to **restore DB from a file**.
- Apply security best practices and include automated tests.

---

## 8. Git Commits & Comments
Role: Git commit quality reviewer
Instructions:
- Build a Claude custom command that enforces Git best practices:
  - Stage relevant files.
  - Write meaningful, clear commit messages.
  - Push commits to `main` branch unless another branch is more appropriate.
- Advise if commits should go to `main` or a separate branch for testing.


## 9. Test fixes

# Spring Boot / Spock Test Failure Analysis and Fixing Instructions

## Role
You are an **expert Spock tester**. Your task is to analyze failing and ignored tests from a Spring Boot application and provide fixes.
The fixes must:
- Address root causes in both **test code** only.
- Follow **Spock and Groovy testing best practices**
- Maintain test clarity and readability
- Avoid breaking existing passing tests
- ./gradlew functionalTest - this is how the tests are executed.

---

## Task Requirements
1. **Analyze each failing test** and determine:
   - The likely root cause
   - The necessary code or logic changes
   - How to ensure the test passes without weakening its intent

2. **Analyze each ignored test** and:
   - Identify why it might have been ignored
   - Recommend changes to re-enable and pass the test
   - Remove unnecessary `@Ignore` annotations if possible

3. **Suggest any refactoring** to improve overall test structure, maintainability, and performance.

---

## Failing Tests
  - ./gradlew functionalTest --tests "finance.controllers.PaymentControllerSpec"  PaymentControllerSpec: "test insert Payment - pay a debit account"
  - ReceiptImageControllerSpec: "test insert receiptImage - png" (different test now failing)
  - TransactionControllerSpec: "test update Transaction"
  - TransactionJpaSpec: "test transaction repository - insert 2 records with duplicate guid - throws an exception"

## 10. When I return a 401 to my application log it
- Role: Act as an expert SpringBoot Programmer who knows all about Kotlin and writting APIs
- Action:
- When a user calls any api for example /api/me and there is a 401 returned to the application -- log it in the spring boot logs use best practices and security measures when building any changes





Instructions
Review the provided Kotlin code for security flaws.

Document all findings in a file called SECURITY.md.

Order findings by importance/severity (highest risk first).

Identify and reference known CVEs if relevant.

Highlight coding flaws that could lead to vulnerabilities (e.g., injection risks, insecure API usage, unsafe deserialization, improper authentication/authorization checks).

Explicitly note where data validation is performed and whether it occurs before trusting or using the data.

Include both logical vulnerabilities and misconfigurations.

Additional Security Checks to Consider
A good security engineer should also check for:

Input validation gaps — missing or incomplete sanitization of user input.

Injection risks — SQL injection, command injection, expression language injection.

Authentication flaws — weak password policies, missing multi-factor auth enforcement.

Authorization issues — improper access control, privilege escalation potential.

Data exposure — sensitive information logged, returned in responses, or stored without encryption.

Cryptography misuse — hardcoded keys, weak encryption algorithms, improper random number generation.

Insecure deserialization — untrusted data being deserialized without safeguards.

Error handling leaks — stack traces or internal details exposed to users.

Dependency vulnerabilities — outdated libraries with known CVEs.

Race conditions — concurrency issues that could cause inconsistent or exploitable states.

API security — missing authentication, weak tokens, or insufficient rate limiting.

Session management issues — insecure cookies, missing HttpOnly or Secure flags, long-lived sessions.

Logging & monitoring gaps — inability to detect or audit malicious behavior.

---

## 11. Error Handling Improvements

### Critical Error Handling Issues Found:

#### **High Priority**

1. **Service Layer Runtime Exceptions**
   - **Location**: `TransactionService.kt:259, 275, 319, 341, 347, 358, 365, 375, 455, 469`
   - **Issue**: Multiple RuntimeException throws without proper exception types
   - **Risk**: Generic exceptions make error diagnosis difficult and violate Spring Boot best practices
   - **Solution**: Create specific custom exceptions (e.g., `TransactionNotFoundException`, `InvalidTransactionStateException`, `AccountValidationException`)

2. **Security Filter Error Handling**
   - **Location**: `JwtAuthenticationFilter.kt:62-65`
   - **Issue**: JWT exceptions only logged but not tracked for security monitoring
   - **Risk**: No audit trail for authentication failures, potential security blind spot
   - **Solution**: Add security event logging and metrics for failed authentication attempts

3. **Database Query Error Handling**
   - **Location**: Service layer database operations
   - **Issue**: No timeout handling, connection pool exhaustion not handled
   - **Risk**: Application hanging on slow queries, resource exhaustion
   - **Solution**: Add query timeouts, circuit breakers, and connection pool monitoring

#### **Medium Priority**

4. **Input Validation Error Messages**
   - **Location**: `BaseController.kt:28-31, 35-38`
   - **Issue**: Generic error messages expose internal exception details
   - **Risk**: Information disclosure, poor user experience
   - **Solution**: Implement structured error responses with sanitized messages

5. **Image Processing Error Handling**
   - **Location**: `TransactionService.kt:388-394`
   - **Issue**: IIOException caught but empty byte array returned silently
   - **Risk**: Silent failures in image processing
   - **Solution**: Log errors appropriately and return meaningful error responses

6. **Camel Route Error Handling**
   - **Location**: `TransactionToDatabaseRouteBuilder.kt:28-31`
   - **Issue**: Only logs InvalidPayloadException, no dead letter queue
   - **Risk**: Lost messages on processing failures
   - **Solution**: Implement dead letter queue and retry mechanisms

#### **Low Priority**

7. **Optional.get() Usage**
   - **Location**: `TransactionService.kt:267`
   - **Issue**: Direct Optional.get() calls without presence check
   - **Risk**: NoSuchElementException if Optional is empty
   - **Solution**: Use orElseThrow() with meaningful exceptions

8. **Incomplete Error Metrics**
   - **Location**: Various service methods
   - **Issue**: Some error paths missing metrics (TODO comments present)
   - **Risk**: Incomplete monitoring and alerting
   - **Solution**: Add metrics for all error scenarios

### **Recommended Implementation Order:**

1. **Create Custom Exception Hierarchy**
   ```kotlin
   // Create custom exceptions in finance.exceptions package
   class TransactionNotFoundException(message: String) : RuntimeException(message)
   class InvalidTransactionStateException(message: String) : IllegalArgumentException(message)
   class AccountValidationException(message: String) : ValidationException(message)
   ```

2. **Implement Global Exception Handler**
   ```kotlin
   @ControllerAdvice
   class GlobalExceptionHandler {
       @ExceptionHandler(TransactionNotFoundException::class)
       fun handleTransactionNotFound(ex: TransactionNotFoundException): ResponseEntity<ErrorResponse>
       
       @ExceptionHandler(JwtException::class)
       fun handleJwtException(ex: JwtException): ResponseEntity<ErrorResponse>
   }
   ```

3. **Add Circuit Breakers for Database Operations**
   - Use Spring Cloud Circuit Breaker for database resilience
   - Implement fallback mechanisms for critical operations

4. **Enhanced Security Error Logging**
   - Add structured logging for authentication failures
   - Implement security event monitoring with Micrometer

5. **Dead Letter Queue for Camel Routes**
   - Configure error handling and retry policies
   - Implement message recovery mechanisms

### **Testing Requirements:**
- Unit tests for all custom exceptions
- Integration tests for error scenarios
- Security tests for authentication failure handling
- Performance tests under error conditions

### **Monitoring Requirements:**
- Add custom metrics for each exception type
- Create alerts for unusual error patterns
- Dashboard for error trends and patterns

## 12. Return 409 Conflict on Duplicate Record Insertion
Role: Expert Spring Boot Kotlin Developer
Instructions:
- Modify the exception handling in the specified controller methods to catch database constraint violation exceptions (e.g., `DataIntegrityViolationException`).
- When such an exception is caught, the API should return an HTTP status code of `409 Conflict`.
- The response body should contain a clear error message indicating that a duplicate record was found.

### Controllers and Methods to Update:

- **`AccountController.insertAccount`**: Currently returns a generic 500 error on duplicate account insertion.
- **`CategoryController.insertCategory`**: Currently returns a generic 500 error on duplicate category insertion.
- **`DescriptionController.insertDescription`**: Currently returns a generic 500 error on duplicate description insertion.
- **`TransactionController.insertTransaction`**: Currently returns a generic 500 error on duplicate transaction insertion.
- **`TransactionController.insertFutureTransaction`**: Currently returns a generic 500 error on duplicate transaction insertion.
- **`UserController.signUp`**: Currently returns a generic 400 bad request on duplicate user insertion.
- **`ParameterController.insertParameter`**: Currently returns a generic 500 error on duplicate parameter insertion.

---

## 13. Dependency Updates Required

### Dependencies with Later Release Versions:

#### **High Priority Updates (Stable Releases):**
- **Jakarta EE Platform API**: `jakarta.platform:jakarta.jakartaee-api` [10.0.0 → 11.0.0]
- **jOOQ Plugin**: `nu.studer.jooq:nu.studer.jooq.gradle.plugin` [10.0 → 10.1.1]
- **Flyway**: `org.flywaydb.flyway:org.flywaydb.flyway.gradle.plugin` [11.9.1 → 11.11.0]
- **jOOQ Code Generation**: `org.jooq:jooq-codegen` [3.19.22 → 3.20.6]
- **jOOQ Core**: `org.jooq:jooq` [3.19.22 → 3.20.6]

#### **Medium Priority Updates (May require testing):**
- **Logback Classic**: `ch.qos.logback:logback-classic` [1.5.15 → 1.5.18]
- **Jackson Core**: `com.fasterxml.jackson.core:jackson-core` [2.19.1 → 2.20.0-rc1]
- **Jackson Databind**: `com.fasterxml.jackson.core:jackson-databind` [2.19.1 → 2.20.0-rc1]
- **Jackson Annotations**: `com.fasterxml.jackson.core:jackson-annotations` [2.19.1 → 3.0-rc5]
- **Jackson Kotlin Module**: `com.fasterxml.jackson.module:jackson-module-kotlin` [2.19.1 → 2.20.0-rc1]
- **Resilience4j**: All modules [2.2.0 → 2.3.0]
- **Micrometer InfluxDB**: `io.micrometer:micrometer-registry-influx` [1.14.8 → 1.16.0-M2]
- **Thumbnailator**: `net.coobird:thumbnailator` [0.4.19 → 0.4.20]
- **Tomcat JDBC**: `org.apache.tomcat:tomcat-jdbc` [10.1.31 → 11.0.10]
- **Flyway Core**: `org.flywaydb:flyway-core` [11.9.1 → 11.11.0]
- **Flyway PostgreSQL**: `org.flywaydb:flyway-database-postgresql` [11.9.1 → 11.11.0]
- **JAXB Runtime**: `org.glassfish.jaxb:jaxb-runtime` [2.3.1 → 4.0.5]
- **JAXB API**: `javax.xml.bind:jaxb-api` [2.3.1 → 2.4.0-b180830.0359]
- **Hibernate Core**: `org.hibernate:hibernate-core` [6.6.18.Final → 7.1.0.Final]
- **Scala Library**: `org.scala-lang:scala-library` [2.13.16 → 2.13.17-M1]
- **Spock Framework**: `org.spockframework:spock-core` [2.3-groovy-4.0 → 2.4-M6-groovy-4.0]
- **Testcontainers**: `org.testcontainers:spock` [1.20.4 → 1.21.3]

#### **Low Priority Updates (Beta/RC versions):**
- **Kotlin**: All Kotlin dependencies [2.2.0 → 2.2.20-Beta2]
- **Spring Boot**: All Spring Boot dependencies [3.5.3 → 4.0.0-M1] *(Major version - requires extensive testing)*
- **Spring Security**: All Spring Security dependencies [6.5.1 → 7.0.0-M1] *(Major version - requires extensive testing)*
- **Apache Groovy**: `org.apache.groovy:groovy` [4.0.25 → 5.0.0-rc-1]

#### **Gradle Update:**
- **Gradle**: [8.14.3 → 9.1.0-rc-1] *(Major version - requires compatibility testing)*

### **Update Strategy:**
1. Start with high priority stable releases
2. Test medium priority updates in development environment
3. Delay major version updates (Spring Boot 4.x, Spring Security 7.x) until stable
4. Update Gradle after ensuring all plugins are compatible

### **Notes:**
- Spring Boot 4.0.0-M1 and Spring Security 7.0.0-M1 are milestone releases - not recommended for production
- Jackson 3.x represents a major version change - requires API compatibility review
- Hibernate 7.x is a major version - requires database compatibility testing
- Some dependencies are at latest stable versions and don't require updates

---

## 14. HTTP Response Code Analysis & Recommendations

### **Analysis Summary**
Professional review of Spring Boot controllers revealed both **correct** and **problematic** HTTP status code usage patterns. The 404 and 409 handling is implemented correctly, but several issues exist with 400 vs 500 response codes.

### **✅ Correctly Implemented Patterns:**

#### **404 NOT_FOUND Usage (Excellent)**
- **AccountController.kt:88-91, 131-134**: Proper 404 for missing accounts
- **CategoryController.kt:42-45, 109-112**: Correct 404 for missing categories  
- **TransactionController.kt:65-69, 207-210**: Appropriate 404 for missing transactions
- **ParameterController.kt:43-46, 109-112**: Good 404 for missing parameters
- **DescriptionController.kt:60-63, 100-103**: Proper 404 for missing descriptions

#### **409 CONFLICT Usage (Excellent)**
- **AccountController.kt:110-113**: DataIntegrityViolationException → 409 CONFLICT
- **CategoryController.kt:88-91**: Duplicate category handling
- **TransactionController.kt:129-132**: Duplicate transaction handling
- **ParameterController.kt:65-68**: Duplicate parameter handling  
- **DescriptionController.kt:82-85**: Duplicate description handling
- **UserController.kt:30-33**: Duplicate user handling

### **❌ Critical Issues Found:**

#### **1. Inappropriate BAD_REQUEST (400) Usage**
**Location**: Multiple controllers  
**Issue**: Catching `ResponseStatusException` and re-throwing as BAD_REQUEST
```kotlin
// INCORRECT PATTERN - AccountController.kt:113-116
} catch (ex: ResponseStatusException) {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert account: ${ex.message}", ex)
}
```
**Problem**: This converts legitimate 500 errors to 400, misleading clients about error nature  
**Severity**: **HIGH** - Violates HTTP semantics

#### **2. Missing Input Validation Error Handling**
**Location**: `TransactionController.kt:171-173`  
**Issue**: Manual null/blank checks throw BAD_REQUEST, but missing comprehensive validation
```kotlin
if (accountNameOwner.isNullOrBlank() || guid.isNullOrBlank()) {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Both accountNameOwner and guid are required")
}
```
**Problem**: Inconsistent validation approach across endpoints  
**Severity**: **MEDIUM**

#### **3. Generic Exception Handling Issues**
**Location**: Multiple controllers (UpdateAccount, UpdateCategory, etc.)  
**Issue**: All exceptions → 500 INTERNAL_SERVER_ERROR without differentiation
```kotlin
} catch (ex: Exception) {
    throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update account: ${ex.message}", ex)
}
```
**Problem**: Doesn't distinguish between client errors (400) and server errors (500)  
**Severity**: **MEDIUM**

#### **4. TransactionController Specific Issues**
**Location**: `TransactionController.kt:110-117`  
**Issue**: IllegalArgumentException correctly mapped to 400, but inconsistent with other controllers
```kotlin
} catch (ex: IllegalArgumentException) {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transaction state: $transactionStateValue", ex)
}
```
**Problem**: This is correct, but other controllers don't handle IllegalArgumentException  
**Severity**: **LOW** - Inconsistency issue

### **🔧 Recommended Fixes:**

#### **Priority 1: Fix ResponseStatusException Re-throwing**
**Controllers**: AccountController, CategoryController, DescriptionController, ParameterController  
**Action**: Remove catch blocks that re-throw ResponseStatusException as BAD_REQUEST
```kotlin
// REMOVE THIS PATTERN:
} catch (ex: ResponseStatusException) {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to insert: ${ex.message}", ex)
}
```

#### **Priority 2: Implement Proper Validation Exception Handling**
**Action**: Add consistent validation error handling across all controllers
```kotlin
} catch (ex: jakarta.validation.ValidationException) {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation error: ${ex.message}", ex)
} catch (ex: IllegalArgumentException) {
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid input: ${ex.message}", ex)
}
```

#### **Priority 3: Add Bean Validation**
**Action**: Use `@Valid` annotation on request bodies and implement proper ConstraintViolationException handling
```kotlin
@PostMapping("/insert")
fun insertAccount(@Valid @RequestBody account: Account): ResponseEntity<Account>
```

#### **Priority 4: Create Custom Exception Types**
**Action**: Create domain-specific exceptions to improve error handling granularity
```kotlin
// finance.exceptions package
class InvalidAccountStateException(message: String) : IllegalArgumentException(message)
class AccountNotFoundException(message: String) : RuntimeException(message)
```

### **📊 HTTP Status Code Guidelines:**

#### **400 BAD_REQUEST - Client Error**
- Invalid request format/syntax
- Missing required parameters
- Validation failures
- Invalid enum values
- Malformed JSON

#### **404 NOT_FOUND - Resource Not Found**
- Entity doesn't exist by ID/name
- Valid request format but resource missing

#### **409 CONFLICT - Resource Conflict**
- Duplicate key violations
- Concurrent modification conflicts
- Business rule violations preventing operation

#### **500 INTERNAL_SERVER_ERROR - Server Error**
- Database connectivity issues
- Unexpected runtime exceptions
- System configuration problems
- Third-party service failures

### **⚠️ Security Considerations:**
- Never expose internal exception details in error messages
- Log sensitive error details server-side only
- Use generic error messages for client responses
- Implement rate limiting for error-prone endpoints

### **🧪 Testing Requirements:**
- Unit tests for each error scenario
- Integration tests verifying correct HTTP status codes
- Security tests ensuring no information leakage
- Load tests for error handling under stress

### **📈 Monitoring Recommendations:**
- Add metrics for each HTTP status code
- Alert on unusual 500 error rates
- Track 400 error patterns for validation improvements
- Monitor 409 conflicts for business insights

---

## 16. Security Hardening: CSRF Protection Implementation

### Executive Summary

This document outlines the implementation plan for enabling Cross-Site Request Forgery (CSRF) protection across the finance ecosystem consisting of:
- **Backend**: Spring Boot API (`raspi-finance-endpoint`)
- **Frontend**: NextJS web application (`nextjs-website`)

### Current Security Assessment

#### Backend (Spring Boot) Security State
- **Authentication**: JWT-based stateless authentication
- **CSRF Status**: ❌ **DISABLED** (`.csrf { it.disable() }` in WebSecurityConfig.kt:37)
- **CORS**: ✅ Configured with explicit origin allowlist
- **Sessions**: Stateless (SessionCreationPolicy.STATELESS)
- **Cookies**: JWT tokens delivered via HTTP-only cookies

#### Frontend (NextJS) Security State
- **Authentication**: Cookie-based with proxy middleware
- **Request Handling**: Middleware proxy to backend API
- **CORS**: Handled at middleware level
- **Cookie Management**: Secure attributes configured for production

#### Current Natural CSRF Protections
✅ HTTP-only cookies (prevents XSS access to tokens)
✅ SameSite cookie attributes configured
✅ CORS origin allowlist restricts cross-origin requests
✅ Middleware validates request origins

### CSRF Protection Strategy

#### Approach: Double Submit Cookie Pattern with Custom Headers

**Rationale**:
- Maintains stateless architecture
- Compatible with JWT authentication
- Provides strong CSRF protection
- Minimal performance impact

### Implementation Plan

#### Phase 1: Backend CSRF Infrastructure (Week 1-2)

##### 1.1 Enable Spring Security CSRF Protection
**File**: `src/main/kotlin/finance/configurations/WebSecurityConfig.kt`

**Changes Required**:
```kotlin
// Replace line 37
.csrf { it.disable() }

// With
.csrf { csrf ->
    csrf
        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        .csrfTokenRequestHandler(SpaCsrfTokenRequestHandler())
        .ignoringRequestMatchers("/api/login", "/api/register", "/api/pending/transaction/insert")
}
```

##### 1.2 Create Custom CSRF Handler
**File**: `src/main/kotlin/finance/configurations/SpaCsrfTokenRequestHandler.kt` (NEW)

**Purpose**: Custom handler for SPA CSRF token delivery

##### 1.3 Update CORS Headers for CSRF
**File**: `src/main/kotlin/finance/configurations/WebSecurityConfig.kt`

**Changes**: Add CSRF token header to allowedHeaders list:
```kotlin
allowedHeaders = listOf(
    "Content-Type",
    "Accept", 
    "Cookie",
    "X-Requested-With",
    "X-CSRF-TOKEN"  // ADD THIS
)
```

##### 1.4 Add CSRF Exception Handling
**File**: `src/main/kotlin/finance/configurations/SecurityExceptionHandler.kt` (NEW)

**Purpose**: Provide clear error responses for CSRF failures

#### Phase 2: Frontend CSRF Integration (Week 2-3)

##### 2.1 Update Authentication Context
**File**: `nextjs-website/components/AuthProvider.tsx`

**Changes**: Add CSRF token state management and automatic token refresh

##### 2.2 Create CSRF Token Hook
**File**: `nextjs-website/hooks/useCsrfToken.ts` (NEW)

**Purpose**: Centralized CSRF token management with automatic refresh

##### 2.3 Update Middleware Proxy
**File**: `nextjs-website/middleware.js`

**Changes**:
- Forward CSRF tokens in proxied requests
- Handle CSRF token extraction from cookies
- Add token to request headers

##### 2.4 Update All API Hooks
**Files**: `nextjs-website/hooks/use*.ts` (All mutation hooks)

**Changes**: Automatic CSRF token inclusion in requests

#### Phase 3: Security Hardening (Week 3-4)

##### 3.1 Enhanced CORS Security
**File**: `src/main/kotlin/finance/configurations/WebSecurityConfig.kt`

**Changes**:
- Remove development localhost from production CORS
- Implement environment-specific CORS configuration
- Add request origin validation middleware

##### 3.2 Implement Additional Security Headers
**File**: `src/main/kotlin/finance/configurations/SecurityHeadersFilter.kt` (NEW)

**Headers to Add**:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 1; mode=block`
- `Referrer-Policy: strict-origin-when-cross-origin`

##### 3.3 Rate Limiting Enhancement
**File**: `src/main/kotlin/finance/configurations/RateLimitingFilter.kt`

**Changes**: Stricter limits on state-changing operations

#### Phase 4: Testing & Validation (Week 4-5)

##### 4.1 Security Testing
- CSRF attack simulation tests
- Cross-origin request validation
- Token rotation testing
- Browser compatibility testing

##### 4.2 Performance Testing
- CSRF token generation overhead
- Request latency impact
- Memory usage validation

### Deployment Schedule

#### Development Environment (Week 1)
- **Target**: Local development setup
- **Scope**: Backend CSRF implementation
- **Testing**: Unit and integration tests
- **Rollback**: Immediate if breaking changes

#### Staging Environment (Week 2)
- **Target**: Staging deployment
- **Scope**: Full CSRF implementation
- **Testing**: End-to-end functional testing
- **Validation**: Security penetration testing
- **Rollback**: 1-hour RTO

#### Production Deployment (Week 3)

##### Pre-Production Checklist
- [ ] All tests passing (unit, integration, functional, security)
- [ ] Performance benchmarks validated
- [ ] Security audit completed
- [ ] Rollback procedures tested
- [ ] Monitoring alerts configured

##### Production Deployment Strategy
**Blue-Green Deployment**:
1. **Green Environment**: Deploy with CSRF enabled
2. **Traffic Split**: 10% traffic to green environment
3. **Monitoring**: 24-hour observation period
4. **Full Cutover**: If metrics stable
5. **Blue Retirement**: After 48-hour stability

##### Production Timeline
- **Day 1**: Deploy to green environment (10% traffic)
- **Day 2**: Monitor metrics and security logs
- **Day 3**: Increase to 50% traffic if stable
- **Day 4**: Full cutover if all metrics green
- **Day 5-6**: Monitor and document

### Risk Assessment & Mitigation

#### High Risk: Application Breakage
**Risk**: CSRF implementation breaks existing functionality
**Mitigation**: 
- Comprehensive testing in staging
- Gradual rollout strategy
- Immediate rollback capability
- Feature flags for CSRF enforcement

#### Medium Risk: Performance Impact
**Risk**: CSRF token generation affects API performance
**Mitigation**:
- Performance testing before deployment
- Token caching strategy
- Monitoring dashboards
- Automatic scaling triggers

#### Medium Risk: Browser Compatibility
**Risk**: CSRF implementation fails in certain browsers
**Mitigation**:
- Cross-browser testing
- Progressive enhancement approach
- Fallback mechanisms
- User agent detection

#### Low Risk: User Experience Degradation
**Risk**: CSRF errors confuse users
**Mitigation**:
- Clear error messages
- Automatic token refresh
- Graceful failure handling
- User education

### Success Metrics

#### Security Metrics
- Zero successful CSRF attacks in production
- 100% request coverage with CSRF protection
- Reduced security audit findings

#### Performance Metrics
- < 5ms additional latency per request
- < 1% increase in server resource usage
- > 99.9% uptime during deployment

#### Functional Metrics
- Zero breaking changes to existing workflows
- < 0.1% increase in support tickets
- 100% feature parity maintained

### Configuration Management

#### Environment Variables
```bash
# Backend
CSRF_ENABLED=true                    # Production: true, Dev: false
CSRF_TOKEN_VALIDITY_HOURS=24         # Token lifetime
CSRF_SECURE_COOKIE=true              # Production: true, Dev: false

# Frontend  
NEXT_PUBLIC_CSRF_ENABLED=true        # Enable CSRF handling
CSRF_TOKEN_REFRESH_INTERVAL=3600000  # 1 hour in milliseconds
```

#### Profile-Specific Configuration
- **Development**: CSRF disabled for easier testing
- **Integration**: CSRF enabled with relaxed validation
- **Staging/Production**: Full CSRF enforcement

### Monitoring & Alerting

#### Security Monitoring
- CSRF token validation failures
- Suspicious cross-origin requests
- Token generation/validation latency
- Failed authentication attempts

#### Performance Monitoring
- API response times (pre/post CSRF)
- Memory usage patterns
- Token generation rate
- Cache hit/miss ratios

#### Alert Thresholds
- **Critical**: > 10 CSRF failures/minute
- **Warning**: > 100ms token validation latency
- **Info**: CSRF token refresh events

### Training & Documentation

#### Development Team Training (Week 1)
- CSRF attack vectors and prevention
- Spring Security CSRF implementation
- Testing strategies for CSRF protection
- Debugging CSRF issues

#### Operations Team Training (Week 2)
- CSRF-related monitoring
- Incident response procedures
- Performance impact assessment
- Rollback procedures

#### Updated Documentation
- API documentation with CSRF requirements
- Frontend integration guide
- Security best practices
- Troubleshooting guide

### Compliance Considerations

#### Security Standards Alignment
- **OWASP Top 10**: Addresses A01:2021 - Broken Access Control
- **NIST Cybersecurity Framework**: Implements protection controls
- **PCI DSS**: Enhances payment data protection (if applicable)

#### Audit Trail
- All CSRF-related changes logged
- Security testing results documented
- Deployment approvals recorded
- Post-deployment validation completed

### Emergency Procedures

#### Immediate Rollback Triggers
- Application completely inaccessible
- > 50% increase in error rates
- Critical security vulnerability discovered
- Performance degradation > 20%

#### Rollback Procedure
1. **Immediate**: Revert to previous deployment
2. **Communication**: Notify stakeholders within 15 minutes
3. **Investigation**: Root cause analysis within 2 hours
4. **Documentation**: Incident report within 24 hours

#### Contact Information
- **Security Team Lead**: [Contact Info]
- **DevOps On-Call**: [Contact Info]
- **Product Owner**: [Contact Info]

### Approval Sign-offs

#### Technical Review
- [ ] Security Engineering Team
- [ ] Backend Development Team  
- [ ] Frontend Development Team
- [ ] DevOps/Infrastructure Team

#### Business Approval
- [ ] Product Owner
- [ ] Security Officer
- [ ] Operations Manager

#### Final Deployment Approval
- [ ] Technical Lead
- [ ] Security Lead
- [ ] Business Stakeholder

---

**Document Version**: 1.0  
**Last Updated**: 2025-08-14  
**Next Review**: 2025-09-14  
**Owner**: Security Engineering Team


## 18. Integration Test Fail
DatabaseResilienceIntSpec. test circuit breaker bean configuration
DatabaseResilienceIntSpec. test circuit breaker metrics and events
DatabaseResilienceIntSpec. test circuit breaker with successful database operation
DatabaseResilienceIntSpec. test concurrent database operations with resilience
DatabaseResilienceIntSpec. test database health indicator integration
DatabaseResilienceIntSpec. test resilience configuration executeWithResilience method
DatabaseResilienceIntSpec. test resilience configuration with database transaction
DatabaseResilienceIntSpec. test retry bean configuration
GraphQLIntegrationSpec. test GraphQL data fetcher service integration
GraphQLIntegrationSpec. test GraphQL endpoint accessibility
GraphQLIntegrationSpec. test GraphQL mutation service integration
GraphQLIntegrationSpec. test GraphQL schema introspection capability
GraphQLIntegrationSpec. test GraphiQL endpoint accessibility
GraphQLIntegrationSpec. test service layer integration for GraphQL data fetchers
ProcessorIntegrationSpec. test insert transaction processor with list of transactions
ProcessorIntegrationSpec. test insert transaction processor with valid transaction
ProcessorIntegrationSpec. test json transaction processor with multiple transactions
ProcessorIntegrationSpec. test json transaction processor with valid json
ProcessorIntegrationSpec. test processor integration with transaction validation
ProcessorIntegrationSpec. test processor performance with large transaction set
ProcessorIntegrationSpec. test processor with different transaction types and states
ProcessorIntegrationSpec. test string transaction processor with csv-like data
AccountRepositoryIntSpec. test account constraint violations
AccountRepositoryIntSpec. test account deletion
AccountRepositoryIntSpec. test account null constraint violations
AccountRepositoryIntSpec. test account query performance
AccountRepositoryIntSpec. test account repository basic CRUD operations
AccountRepositoryIntSpec. test account update operations
AccountRepositoryIntSpec. test find accounts by account type
AccountRepositoryIntSpec. test find accounts by active status
AccountRepositoryIntSpec. test find accounts by active status and account type
AccountRepositorySimpleIntSpec. test account constraint violations
AccountRepositorySimpleIntSpec. test account deletion
AccountRepositorySimpleIntSpec. test account performance with multiple accounts
AccountRepositorySimpleIntSpec. test account repository basic CRUD operations
AccountRepositorySimpleIntSpec. test account repository custom queries
AccountRepositorySimpleIntSpec. test account repository update operations
AccountRepositorySimpleIntSpec. test find accounts by active status
TransactionRepositoryIntSpec. test count operations for category and description
TransactionRepositoryIntSpec. test find by account name owner excluding transaction states
TransactionRepositoryIntSpec. test find transactions by account name owner and active status
TransactionRepositoryIntSpec. test find transactions by category and description
TransactionRepositoryIntSpec. test sum totals for active transactions by account name owner
TransactionRepositoryIntSpec. test transaction constraint violations
TransactionRepositoryIntSpec. test transaction query performance with large dataset
TransactionRepositoryIntSpec. test transaction repository basic CRUD operations
TransactionRepositorySimpleIntSpec. test count operations for category and description
TransactionRepositorySimpleIntSpec. test find by account name owner excluding transaction states
TransactionRepositorySimpleIntSpec. test find transactions by account name owner and active status
TransactionRepositorySimpleIntSpec. test find transactions by category and description
TransactionRepositorySimpleIntSpec. test sum totals for active transactions by account name owner
TransactionRepositorySimpleIntSpec. test transaction query performance with multiple transactions
TransactionRepositorySimpleIntSpec. test transaction repository basic CRUD operations
CamelRouteIntegrationSpec. test camel route metrics and monitoring
CamelRouteIntegrationSpec. test complete file processing workflow
CamelRouteIntegrationSpec. test concurrent file processing
CamelRouteIntegrationSpec. test direct route transaction processing
CamelRouteIntegrationSpec. test file processing performance with multiple files
CamelRouteIntegrationSpec. test json file reader route exists and is active
CamelRouteIntegrationSpec. test json file writer route exists and is active
CamelRouteIntegrationSpec. test multiple transactions in single file processing
CamelRouteIntegrationSpec. test transaction to database route exists and is active
SecurityIntegrationSimpleSpec. test CORS headers handling
SecurityIntegrationSimpleSpec. test concurrent user operations
SecurityIntegrationSimpleSpec. test protected endpoint access without authentication
SecurityIntegrationSimpleSpec. test user authentication data integrity
SecurityIntegrationSimpleSpec. test user service integration
SecurityIntegrationSimpleSpec. test user service validation and constraints
SecurityIntegrationSpec. test CORS headers in security configuration
SecurityIntegrationSpec. test JWT token claims and structure
SecurityIntegrationSpec. test JWT token refresh scenarios
SecurityIntegrationSpec. test JWT token with different authorities
SecurityIntegrationSpec. test concurrent JWT token operations
SecurityIntegrationSpec. test invalid JWT token validation
SecurityIntegrationSpec. test protected endpoint access with expired JWT token
SecurityIntegrationSpec. test protected endpoint access with invalid JWT token
SecurityIntegrationSpec. test protected endpoint access with valid JWT token
SecurityIntegrationSpec. test protected endpoint access without authentication
SecurityIntegrationSpec. test user detail service load user by username
SecurityIntegrationWorkingSpec. test CORS headers handling
SecurityIntegrationWorkingSpec. test concurrent user operations
SecurityIntegrationWorkingSpec. test protected endpoint access without authentication
SecurityIntegrationWorkingSpec. test user service integration
SecurityIntegrationWorkingSpec. test user service validation and constraints
ExternalIntegrationsSpec. test HTTP request metrics
ExternalIntegrationsSpec. test JVM metrics availability
ExternalIntegrationsSpec. test actuator beans endpoint
ExternalIntegrationsSpec. test actuator env endpoint
ExternalIntegrationsSpec. test actuator health endpoint with detailed information
ExternalIntegrationsSpec. test actuator info endpoint
ExternalIntegrationsSpec. test actuator metrics endpoint accessibility
ExternalIntegrationsSpec. test application health indicators integration
ExternalIntegrationsSpec. test application startup metrics
ExternalIntegrationsSpec. test circuit breaker metrics integration
ExternalIntegrationsSpec. test custom application metrics registration
ExternalIntegrationsSpec. test custom business metrics with MeterService
ExternalIntegrationsSpec. test database connection pool metrics
ExternalIntegrationsSpec. test database metrics with transaction operations
ExternalIntegrationsSpec. test memory and garbage collection metrics
ExternalIntegrationsSpec. test meter registry bean configuration
ExternalIntegrationsSpec. test meter service bean availability
ExternalIntegrationsSpec. test metrics export configuration
ExternalIntegrationsSpec. test performance monitoring integration
ExternalIntegrationsSpec. test thread pool metrics
ExternalIntegrationsSpec. test transaction metrics integration
ServiceLayerIntegrationSpec. test account service integration with totals calculation
ServiceLayerIntegrationSpec. test category service integration with transaction relationships
ServiceLayerIntegrationSpec. test cross-service integration with transaction and account updates
ServiceLayerIntegrationSpec. test description service integration with transaction relationships
ServiceLayerIntegrationSpec. test payment service integration
ServiceLayerIntegrationSpec. test receipt image service integration
ServiceLayerIntegrationSpec. test service layer error handling and validation
ServiceLayerIntegrationSpec. test service layer performance with bulk operations
ServiceLayerIntegrationSpec. test service layer transaction rollback on failure
ServiceLayerIntegrationSpec. test transaction service integration with database operations
ServiceLayerIntegrationSpec. test transfer service integration
ServiceLayerIntegrationSpec. test validation amount service integration


