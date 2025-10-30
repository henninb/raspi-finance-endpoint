# REST Test Brittleness - Root Cause Analysis & Fixes

**Date**: October 30, 2025
**Status**: ⚠️ PARTIAL FIX APPLIED - Additional configuration needed

---

## Executive Summary

Functional REST tests are failing due to JSON serialization/deserialization mismatches between test data and controller expectations. The root causes are:

1. ✅ **FIXED**: Type mismatch in `Transaction.kt` no-arg constructor
2. ✅ **FIXED**: Missing `@JsonInclude` annotations on entities
3. ⚠️ **IN PROGRESS**: Missing Jackson configuration for `func` Spring profile
4. ⚠️ **IDENTIFIED**: Smart builders setting non-null values for fields that should be excluded

---

## Root Causes Identified

### 1. Type Mismatch in Transaction.kt (FIXED ✅)

**Problem**:
```kotlin
// Transaction.kt line 131 - BEFORE
constructor() : this(
    0L,                      // ✅ transactionId: Long
    "",                      // ✅ guid: String
    0,                       // ❌ accountId: Long (Int instead of Long!)
    ...
)
```

**Fix Applied**:
```kotlin
// Transaction.kt line 131 - AFTER
constructor() : this(
    0L,                      // ✅ transactionId: Long
    "",                      // ✅ guid: String
    0L,                      // ✅ accountId: Long (FIXED!)
    ...
)
```

**Files Modified**:
- `src/main/kotlin/finance/domain/Transaction.kt` (line 131)

---

### 2. Missing @JsonInclude Annotations (FIXED ✅)

**Problem**: Entities were serializing ALL fields (including defaults like `transactionId: 0`, `accountId: 0`) which caused deserialization failures.

**Fix Applied**: Added `@JsonInclude(JsonInclude.Include.NON_NULL)` to:
- ✅ `Transaction.kt`
- ✅ `Account.kt`
- ✅ `Payment.kt`
- ✅ `MedicalExpense.kt`

**Configuration Also Applied**: Updated Transaction's internal ObjectMapper:
```kotlin
private val mapper = ObjectMapper().apply {
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    findAndRegisterModules()
}
```

---

### 3. Missing Jackson Configuration for `func` Profile (NOT FIXED ⚠️)

**Problem**: Tests run with `SPRING_PROFILES_ACTIVE=func`, but there's NO Jackson configuration for this profile.

**Current State**:
- ✅ `application-prod.yml` has Jackson config:
  ```yaml
  spring:
    jackson:
      property-naming-strategy: LOWER_CAMEL_CASE
      default-property-inclusion: non_null
      mapper.accept-case-insensitive-enums: true
      time-zone: America/Chicago
  ```
- ❌ `func` profile has NO Jackson configuration
- ❌ Base `application.yml` might not have Jackson config either

**Why This Matters**:
- Tests serialize entities using `entity.toString()` (uses entity's ObjectMapper)
- Controllers deserialize using Spring's global ObjectMapper (configured via application.yml)
- If Spring's ObjectMapper doesn't have `default-property-inclusion: non_null`, it won't know how to handle missing fields

**Fix Needed**: Add Jackson configuration to either:
1. Create `src/test/resources/application-func.yml` with Jackson config
2. Add Jackson config to base `src/main/resources/application.yml`

**Recommended Configuration**:
```yaml
spring:
  jackson:
    default-property-inclusion: non_null
    mapper:
      accept-case-insensitive-enums: true
    deserialization:
      fail-on-null-for-primitives: false  # Allow null → 0L conversion
      fail-on-unknown-properties: false
    time-zone: America/Chicago
```

---

### 4. Smart Builders Setting Non-Null Values for Optional Fields (IDENTIFIED ⚠️)

**Problem**: `SmartTransactionBuilder` sets `dueDate` to a calculated value:

```groovy
// SmartTransactionBuilder.groovy line 38
this.dueDate = new Date(this.transactionDate.time + (7 * 24 * 60 * 60 * 1000L))
```

This causes `dueDate` to be included in JSON even though it's optional:
```json
{
  "guid": "...",
  "dueDate": 1761177539685  // ❌ Should be excluded or null
}
```

**Why This Is a Problem**:
- `dueDate` is sent as epoch timestamp instead of ISO date string
- Spring's ObjectMapper might not properly deserialize this
- Creates unnecessary test data that doesn't reflect real API usage

**Fix Needed**: Modify SmartTransactionBuilder to NOT set `dueDate` by default:
```groovy
// Change line 38 from:
this.dueDate = new Date(this.transactionDate.time + (7 * 24 * 60 * 60 * 1000L))

// To:
this.dueDate = null  // Only set if explicitly requested
```

---

## Test Failure Analysis

### Current Error
```
HttpMessageNotReadableException: Cannot map `null` into type `long`
```

### JSON Being Sent (from test logs)
```json
{
  "guid":"9fc6d3f8-c0be-4f1d-a58a-641bfc6b8dbe",
  "accountType":"credit",
  "transactionType":"expense",
  "accountNameOwner":"account_testfa",
  "transactionDate":"2025-10-15",
  "description":"create_2_test_f581a902",
  "category":"onlinetestf",
  "amount":461.63,
  "transactionState":"cleared",
  "dueDate":1761177539685
}
```

### What's Missing from JSON (Good!)
- ✅ `transactionId` (excluded - auto-generated)
- ✅ `accountId` (excluded - default 0L)
- ✅ `dateAdded` (excluded - @JsonIgnore)
- ✅ `dateUpdated` (excluded - @JsonIgnore)

### What's Present but Problematic
- ⚠️ `dueDate` as epoch timestamp instead of null or date string

### Where the Error Occurs
1. Test creates Transaction via SmartTransactionBuilder
2. Test serializes via `transaction.toString()` (uses entity's ObjectMapper with NON_NULL)
3. Test POSTs JSON to controller
4. **Controller's Spring ObjectMapper tries to deserialize JSON**
5. Error: "Cannot map `null` into type `long`"

### Hypothesis
Even though the JSON doesn't show any `null` values, Spring's ObjectMapper might be:
1. Trying to set fields that aren't in the JSON to `null`
2. Hitting primitive `long` fields (`transactionId`, `accountId`) that can't accept `null`
3. Failing because Spring's ObjectMapper doesn't know to use default values (0L)

---

## Fixes Applied So Far

### ✅ Completed Fixes

1. **Transaction.kt constructor type mismatch**
   - Changed `0,` to `0L,` on line 131
   - File: `src/main/kotlin/finance/domain/Transaction.kt`

2. **Added @JsonInclude annotations**
   - `Transaction.kt`: `@JsonInclude(JsonInclude.Include.NON_NULL)`
   - `Account.kt`: `@JsonInclude(JsonInclude.Include.NON_NULL)`
   - `Payment.kt`: `@JsonInclude(JsonInclude.Include.NON_DEFAULT)`
   - `MedicalExpense.kt`: `@JsonInclude(JsonInclude.Include.NON_DEFAULT)`

3. **Configured Transaction's ObjectMapper**
   - Added `setSerializationInclusion(JsonInclude.Include.NON_NULL)`
   - Added `findAndRegisterModules()` for proper Date handling

4. **Verified no other type mismatches**
   - Scanned all entities
   - Only Transaction had the Int/Long mismatch

---

## Remaining Fixes Needed

### Priority 1: Add Jackson Configuration for Functional Tests

**Option A: Create func-specific config** (Recommended)
```bash
# Create file
touch src/test/resources/application-func.yml
```

```yaml
# Content
spring:
  jackson:
    default-property-inclusion: non_null
    mapper:
      accept-case-insensitive-enums: true
    deserialization:
      fail-on-null-for-primitives: false
      fail-on-unknown-properties: false
```

**Option B: Add to base application.yml**
If no func-specific config needed, add Jackson config to base `application.yml`.

---

### Priority 2: Fix SmartTransactionBuilder Default Values

**File**: `src/test/functional/groovy/finance/helpers/SmartTransactionBuilder.groovy`

**Change**:
```groovy
// Line 21 - BEFORE
private Date dueDate

// Line 38 in constructor - BEFORE
this.dueDate = new Date(this.transactionDate.time + (7 * 24 * 60 * 60 * 1000L))

// AFTER
private Date dueDate = null  // Don't set by default

// Remove line 38 or change to:
// this.dueDate = null  // Only set if explicitly needed
```

**Also check** if other Smart*Builders have similar issues with optional fields.

---

## Long-Term Pattern Recommendations

### 1. Entity Design Pattern (Standardize on Pattern A)
✅ **Use no-arg constructors with correct types**
- All Long fields must use `0L` not `0`
- All entities should have no-arg constructors OR default values
- Prefer no-arg constructors for consistency

### 2. JSON Serialization Pattern
✅ **Use @JsonInclude(NON_NULL) on all REST entities**
- Excludes null fields
- Works with both entity's ObjectMapper and Spring's ObjectMapper
- Prevents sending auto-generated fields (ID, dates, etc.)

### 3. Jackson Configuration Pattern
✅ **Configure Jackson globally for all profiles**
```yaml
spring:
  jackson:
    default-property-inclusion: non_null
    deserialization:
      fail-on-null-for-primitives: false  # Safety net
```

### 4. Test Data Builder Pattern
✅ **Only set required fields by default**
- Optional fields should be `null` by default
- Use explicit builder methods to set optional fields
- Example: `withDueDate(Date)` instead of auto-calculating

---

## Verification Steps

After applying remaining fixes:

```bash
# Test single failing test
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest \
  --tests "StandardizedTransactionControllerFunctionalSpec.should implement standardized method name: save instead of insertTransaction" \
  --rerun-tasks

# Test all Transaction tests
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest \
  --tests "StandardizedTransactionControllerFunctionalSpec" \
  --rerun-tasks

# Test all failing specs
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest \
  --tests "StandardizedTransactionControllerFunctionalSpec" \
  --tests "AccountValidationSyncFunctionalSpec" \
  --tests "MedicalExpenseControllerExtendedFunctionalSpec" \
  --rerun-tasks
```

---

## Summary of Changes Made

### Files Modified
1. ✅ `src/main/kotlin/finance/domain/Transaction.kt`
   - Fixed constructor type mismatch (line 131)
   - Changed @JsonInclude from NON_EMPTY to NON_NULL (line 57)
   - Configured ObjectMapper with NON_NULL and findAndRegisterModules()

2. ✅ `src/main/kotlin/finance/domain/Account.kt`
   - Added @JsonInclude import
   - Added @JsonInclude(NON_DEFAULT) annotation

3. ✅ `src/main/kotlin/finance/domain/Payment.kt`
   - Added @JsonInclude import
   - Added @JsonInclude(NON_DEFAULT) annotation

4. ✅ `src/main/kotlin/finance/domain/MedicalExpense.kt`
   - Added @JsonInclude import
   - Added @JsonInclude(NON_DEFAULT) annotation

### Files Still Needing Changes
1. ⚠️ `src/test/resources/application-func.yml` (CREATE NEW)
   - Add Jackson configuration for functional tests

2. ⚠️ `src/test/functional/groovy/finance/helpers/SmartTransactionBuilder.groovy`
   - Don't auto-set dueDate (line 38)

---

## Prevention Guidelines

### For Future Entity Development
1. All no-arg constructors must use correct types (`0L` for Long, not `0`)
2. All entities used in REST APIs should have `@JsonInclude(NON_NULL)`
3. All `@JsonIgnore` fields must be properly excluded from serialization

### For Future Test Development
1. SmartBuilders should only set REQUIRED fields by default
2. Optional fields should be `null` unless explicitly set
3. Test JSON should only include fields that real clients would send

### For Code Reviews
1. Check no-arg constructors for type correctness
2. Verify @JsonInclude annotations on new entities
3. Ensure SmartBuilders don't set unnecessary defaults

---

## Related Documentation
- **TEST_CONSOLIDATION_SUMMARY.md**: Test file consolidation (removed duplicates)
- **FUNCTIONAL_TEST_FIX.md**: WebTestClient bean error fix
- **CLAUDE.md**: Project testing standards

---

## Next Actions

1. **Immediate** (15 minutes):
   - Create `application-func.yml` with Jackson config
   - Fix SmartTransactionBuilder dueDate default

2. **Verification** (30 minutes):
   - Run all Transaction functional tests
   - Run all failing functional tests
   - Verify all tests pass

3. **Documentation** (15 minutes):
   - Update this document with final results
   - Create prevention checklist for future development

**Total estimated time to complete**: ~1 hour
