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


## 15. broken tests
ReceiptImageControllerSpec. should retrieve receipt image by id when it exists
TransactionControllerSpec. should fail to update transaction receipt image with invalid data
TransferControllerSpec. test select all transfers
UserControllerSpec. test sign up
UuidControllerSpec. test generate single UUID successfully
ValidationAmountControllerSpec. test insert validation amount successfully
