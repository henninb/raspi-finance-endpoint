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


