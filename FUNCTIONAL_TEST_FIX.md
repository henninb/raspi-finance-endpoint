# Functional Test WebTestClient Bean Error Fix

## Problem Summary

Functional tests extending `BaseControllerFunctionalSpec` fail during initialization with the following error:

```
org.springframework.beans.factory.UnsatisfiedDependencyException: Error creating bean with name 'finance.controllers.AccountControllerFunctionalSpec': Unsatisfied dependency expressed through field 'webTestClient': No qualifying bean of type 'org.springframework.test.web.reactive.server.WebTestClient' available
```

## Root Cause Analysis

### What Happened
- The `BaseControllerFunctionalSpec` class declared a `@Autowired` field for `WebTestClient`:
  ```groovy
  @Autowired
  protected WebTestClient webTestClient
  ```
- **Spring Boot tries to inject this dependency during context initialization**
- **The Spring context does not provide a `WebTestClient` bean** in the functional test configuration
- Spring fails to create the test class instance, causing all tests to fail with `initializationError`

### Why This Is Wrong
1. **Functional tests use `RestTemplate`, not `WebTestClient`**
   - The base class creates a static `RestTemplate` instance on line 51
   - All HTTP requests in functional tests use `RestTemplate.exchange()` methods
   - `WebTestClient` is designed for reactive/WebFlux applications, not traditional Spring MVC

2. **Bean was never used**
   - Search across all functional test files showed `webTestClient` only existed in the base class declaration
   - No test methods referenced this field
   - A helper method `convertWebTestClientResponse()` existed but was never called

3. **Initialization order matters**
   - Spring autowires dependencies **before** any test methods run
   - Missing beans cause immediate initialization failure
   - All tests in the spec fail, even if they don't use the problematic field

## The Fix

### Step 1: Remove the unused `@Autowired` field

**File**: `src/test/functional/groovy/finance/controllers/BaseControllerFunctionalSpec.groovy`

**Before**:
```groovy
@Import([TestSecurityConfig])
class BaseControllerFunctionalSpec extends Specification {
    @Autowired
    protected WebTestClient webTestClient

    @Shared
    @Autowired
    protected Environment environment
```

**After**:
```groovy
@Import([TestSecurityConfig])
class BaseControllerFunctionalSpec extends Specification {
    @Shared
    @Autowired
    protected Environment environment
```

### Step 2: Remove the unused import

**Before**:
```groovy
import org.springframework.web.client.RestTemplate
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.io.IOException
import java.lang.reflect.Field
import org.springframework.test.web.reactive.server.WebTestClient
import java.net.HttpURLConnection
import org.flywaydb.core.Flyway
```

**After**:
```groovy
import org.springframework.web.client.RestTemplate
import org.springframework.http.client.SimpleClientHttpRequestFactory
import java.io.IOException
import java.lang.reflect.Field
import java.net.HttpURLConnection
import org.flywaydb.core.Flyway
```

### Step 3: Remove unused helper method (if exists)

**Remove** (this method referenced WebTestClient types):
```groovy
private ResponseEntity<String> convertWebTestClientResponse(WebTestClient.ResponseSpec responseSpec) {
    def result = responseSpec.returnResult(String.class)
    def status = HttpStatus.valueOf(result.status.value())
    def body = result.responseBodyContent ? new String(result.responseBodyContent) : null
    def headers = result.responseHeaders
    return new ResponseEntity<String>(body, headers, status)
}
```

## Verification

### Test the fix:
```bash
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.AccountControllerFunctionalSpec" --rerun-tasks
```

### Expected result:
```
BUILD SUCCESSFUL
AccountControllerFunctionalSpec - all tests passing
```

## How to Apply This Fix to Other Functional Test Classes

### Detection: Identify affected tests

Search for other functional test files that might have the same issue:

```bash
# Search for WebTestClient usage in functional tests
grep -r "webTestClient" src/test/functional/

# Search for similar autowiring patterns
grep -r "@Autowired.*WebTestClient" src/test/
```

### Application Pattern

**This fix applies to ANY test class that:**
1. Extends `BaseControllerFunctionalSpec`
2. Fails with `UnsatisfiedDependencyException` for `WebTestClient`
3. Uses `RestTemplate` for HTTP operations (not reactive `WebTestClient`)

**Fix checklist for each affected class:**
1. ✅ Remove `@Autowired protected WebTestClient webTestClient` field declaration
2. ✅ Remove `import org.springframework.test.web.reactive.server.WebTestClient`
3. ✅ Remove any unused helper methods referencing `WebTestClient` types
4. ✅ Verify the test uses `RestTemplate` (not `WebTestClient`) for HTTP calls
5. ✅ Run the test with `--rerun-tasks` to verify the fix

### Common Locations to Check

Search in these base test classes:
- `BaseControllerFunctionalSpec.groovy` ✅ **FIXED**
- `BaseIntegrationSpec.groovy` (if integration tests have similar issue)
- Any other shared test base classes in `src/test/*/groovy/finance/controllers/`

## Prevention Guidelines

### For Future Test Development

1. **Use the correct HTTP client for your test type**:
   - **Functional tests (Spring MVC)**: Use `RestTemplate`
   - **Reactive tests (WebFlux)**: Use `WebTestClient`
   - **Integration tests**: Use appropriate client based on application architecture

2. **Don't autowire beans you don't use**:
   - Every `@Autowired` field must have a corresponding bean in the Spring context
   - Remove unused autowired fields to prevent initialization failures
   - Use static factory methods or `@Shared` instances when autowiring isn't needed

3. **Validate test base classes during upgrades**:
   - Spring Boot 4.0 migration may have changed available beans
   - Review all `@Autowired` fields in base test classes after framework upgrades
   - Run full test suite after Spring Boot version changes

## Why This Pattern Works

### Functional Test Architecture
```
BaseControllerFunctionalSpec (base class)
├── Uses RestTemplate (static instance, no Spring autowiring needed)
├── Provides helper methods: insertEndpoint(), selectEndpoint(), deleteEndpoint()
├── Manages JWT token generation and authentication
└── Sets up test data via @Shared autowired beans (TestDataManager, TestFixtures)

AccountControllerFunctionalSpec (child class)
├── Extends BaseControllerFunctionalSpec
├── Tests REST endpoints using inherited RestTemplate
├── No need for WebTestClient (wrong tool for Spring MVC)
└── All HTTP operations work with traditional Spring Web (not reactive)
```

### Key Principle
**Only autowire Spring beans that:**
1. Exist in the test Spring context
2. Are actually used by test code
3. Cannot be created statically or through factory methods

## References

- **Fixed class**: `BaseControllerFunctionalSpec.groovy:43-44` (removed webTestClient field)
- **Test verification**: `AccountControllerFunctionalSpec` (all tests passing)
- **Related documentation**: `CLAUDE.md` - Testing Requirements section
- **Spring Boot docs**: [Testing Spring Boot Applications](https://docs.spring.io/spring-boot/reference/testing/index.html)

## Summary

**Problem**: Autowiring `WebTestClient` in non-reactive functional tests
**Solution**: Remove unused `WebTestClient` field and import
**Impact**: All functional tests extending `BaseControllerFunctionalSpec` now initialize correctly
**Applicability**: Any functional test class with `UnsatisfiedDependencyException` for beans not in Spring context
