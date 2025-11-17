# Transaction Date Off-by-One Bug - Fix Plan

## Executive Summary

This document outlines a comprehensive plan to fix the transaction date off-by-one bug that manifests when creating transactions "early in the morning" (typically between midnight and 6 AM local time). The bug is caused by timezone conversions during date parsing and serialization across the REST API layer.

## Problem Statement

### Symptom
When a user creates a transaction with date `2025-11-17` early in the morning (e.g., 1:00 AM CST), the transaction is sometimes stored in the database as `2025-11-16`.

### Root Cause Analysis

The issue stems from **implicit timezone conversions** during date handling in three critical layers:

1. **Jackson JSON Serialization/Deserialization**
   - Custom `@JsonSetter` methods in domain entities use `SimpleDateFormat.parse()`
   - `SimpleDateFormat` parses "yyyy-MM-dd" strings using the **JVM's default timezone**
   - The parsed `java.util.Date` is then converted to epoch milliseconds
   - `java.sql.Date(millis)` constructor interprets these milliseconds, potentially shifting the date

2. **REST API Controller Layer**
   - `@DateTimeFormat(pattern = "yyyy-MM-dd")` in controllers uses Spring's conversion
   - Converts `java.util.Date` to `java.sql.Date` using `.time` property
   - This conversion can introduce timezone shifts depending on server timezone

3. **Fundamental Issue: java.sql.Date Misuse**
   - `java.sql.Date` is a legacy class that extends `java.util.Date`
   - Despite representing "just a date", it internally stores milliseconds from epoch
   - Timezone of the JVM affects how these milliseconds map to calendar dates

### Why It Happens "Early in the Morning"

When the server timezone is CST (UTC-6) and a user sends "2025-11-17":
1. SimpleDateFormat parses it as `2025-11-17 00:00:00 CST`
2. Converted to epoch: corresponds to `2025-11-17 06:00:00 UTC`
3. If PostgreSQL session or JDBC driver interprets this in a different timezone context, it might extract the date as `2025-11-16`

The bug is more pronounced early in the morning because that's when UTC date and local date differ most significantly.

## Scope of Impact

### Database Tables Affected
The `transaction_date` column (type: `DATE`) appears in **4 core tables**:

1. **t_transaction** - Main transaction table
   - Column: `transaction_date DATE NOT NULL`
   - Constraint: `transaction_constraint UNIQUE (account_name_owner, transaction_date, ...)`
   - Entity: `Transaction.kt`

2. **t_pending_transaction** - Pending transaction staging
   - Column: `transaction_date DATE NOT NULL`
   - Constraint: `unique_pending_transaction_fields UNIQUE (..., transaction_date, ...)`
   - Entity: `PendingTransaction.kt`

3. **t_payment** - Payment records linking accounts
   - Column: `transaction_date DATE NOT NULL`
   - Constraint: `payment_constraint UNIQUE (destination_account, transaction_date, amount)`
   - Entity: `Payment.kt`

4. **t_transfer** - Transfer records between accounts
   - Column: `transaction_date DATE NOT NULL`
   - Constraint: `transfer_constraint UNIQUE (source_account, destination_account, transaction_date, amount)`
   - Entity: `Transfer.kt`

### Code Files Affected

**Domain Entities:**
- `src/main/kotlin/finance/domain/Transaction.kt` (lines 89-166)
- `src/main/kotlin/finance/domain/Payment.kt` (lines 64-98)
- `src/main/kotlin/finance/domain/Transfer.kt` (lines 59-93)
- `src/main/kotlin/finance/domain/PendingTransaction.kt` (assumed similar pattern)
- `src/main/kotlin/finance/domain/MedicalExpense.kt` (serviceDate, paidDate fields)

**DTOs:**
- `src/main/kotlin/finance/controllers/dto/TransactionInputDto.kt`
- `src/main/kotlin/finance/controllers/dto/PaymentInputDto.kt`
- `src/main/kotlin/finance/controllers/dto/TransferInputDto.kt`
- `src/main/kotlin/finance/controllers/dto/MedicalExpenseInputDto.kt`

**REST Controllers:**
- `src/main/kotlin/finance/controllers/TransactionController.kt` (lines 248-262)
- `src/main/kotlin/finance/controllers/MedicalExpenseController.kt` (lines 370-371, 423-424, 642-643)
- All controllers that accept or return entities with `transaction_date`

## Solution Strategy

### Approach: Migrate to `java.time.LocalDate`

Replace all `java.sql.Date` usage with `java.time.LocalDate` - the modern, timezone-safe date representation.

**Why LocalDate?**
- ✅ Represents a pure calendar date (year-month-day) with **no time component**
- ✅ **No timezone** - eliminates all timezone-related bugs
- ✅ Immutable and thread-safe
- ✅ Better API design (part of java.time package introduced in Java 8)
- ✅ Direct compatibility with SQL `DATE` column via JDBC 4.2+
- ✅ Jackson supports LocalDate serialization out-of-the-box

**Benefits:**
1. **Eliminates the bug**: No timezone can affect a value that has no time
2. **Cleaner code**: Remove all custom `@JsonGetter`/`@JsonSetter` methods
3. **Modern best practice**: Aligns with Java standards since 2014
4. **Type safety**: Clear intent that this is a date, not a timestamp

### Alternative Quick Fix (Not Recommended)

If migration to `LocalDate` is deemed too risky, a minimal fix would be:

**Replace `SimpleDateFormat.parse()` with `Date.valueOf()`:**

```kotlin
// Current (BROKEN):
this.transactionDate = Date(simpleDateFormat.parse(stringDate).time)

// Fixed:
this.transactionDate = Date.valueOf(stringDate)
```

`Date.valueOf(String)` is specifically designed to parse "YYYY-MM-DD" strings into `java.sql.Date` without timezone interpretation.

**Why not recommended:**
- Still uses legacy `java.sql.Date` API
- Doesn't prevent future timezone bugs elsewhere
- Requires changes in multiple locations
- Doesn't address controller layer conversion issues

## Implementation Plan

### Phase 1: Core Domain Migration (Estimated: 4-6 hours)

#### Step 1.1: Update Domain Entities

**Files to modify:**
- `Transaction.kt`
- `Payment.kt`
- `Transfer.kt`
- `PendingTransaction.kt`
- `MedicalExpense.kt`

**Changes per entity:**

```kotlin
// BEFORE:
import java.sql.Date
import java.text.SimpleDateFormat

data class Transaction(
    // ...
    @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
    var transactionDate: Date = Date(0),
    // ...
) {
    @JsonGetter("transactionDate")
    fun jsonGetterTransactionDate(): String {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
        return simpleDateFormat.format(this.transactionDate)
    }

    @JsonSetter("transactionDate")
    fun jsonSetterTransactionDate(stringDate: String) {
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.isLenient = false
        this.transactionDate = Date(simpleDateFormat.parse(stringDate).time)
    }
}

// AFTER:
import java.time.LocalDate

data class Transaction(
    // ...
    @Column(name = "transaction_date", columnDefinition = "DATE", nullable = false)
    var transactionDate: LocalDate = LocalDate.now(),
    // ...
)
// Remove @JsonGetter and @JsonSetter - Jackson handles LocalDate natively
```

**Key points:**
- Remove `java.sql.Date` import, add `java.time.LocalDate`
- Change field type from `Date` to `LocalDate`
- **Delete** custom `@JsonGetter` and `@JsonSetter` methods entirely
- Update default values (e.g., `Date(0)` → `LocalDate.now()` or appropriate default)

#### Step 1.2: Update DTOs

**Files to modify:**
- `TransactionInputDto.kt`
- `PaymentInputDto.kt`
- `TransferInputDto.kt`
- `MedicalExpenseInputDto.kt`

**Changes:**

```kotlin
// BEFORE:
import java.sql.Date

data class TransactionInputDto(
    // ...
    @field:NotNull
    @field:ValidDate
    val transactionDate: Date,
    // ...
)

// AFTER:
import java.time.LocalDate

data class TransactionInputDto(
    // ...
    @field:NotNull
    @field:ValidDate
    val transactionDate: LocalDate,
    // ...
)
```

#### Step 1.3: Update Validation

**File:** `src/main/kotlin/finance/utils/ValidDate.kt` (if it exists)

Ensure `@ValidDate` annotation validator works with `LocalDate`:

```kotlin
// Update validator to accept LocalDate
class DateValidator : ConstraintValidator<ValidDate, LocalDate> {
    override fun isValid(value: LocalDate?, context: ConstraintValidatorContext): Boolean {
        // LocalDate is always valid by construction - no invalid states possible
        return true
    }
}
```

Or simply remove `@ValidDate` if it's only checking format (LocalDate parsing handles this).

### Phase 2: REST API Layer Updates (Estimated: 2-3 hours)

#### Step 2.1: Update Controllers

**Files to modify:**
- `TransactionController.kt` (lines 248-262)
- `MedicalExpenseController.kt` (multiple endpoints)

**Changes:**

```kotlin
// BEFORE:
fun findByDateRange(
    @RequestParam("startDate") @DateTimeFormat(pattern = "yyyy-MM-dd") start: java.util.Date,
    @RequestParam("endDate") @DateTimeFormat(pattern = "yyyy-MM-dd") end: java.util.Date,
    // ...
): ResponseEntity<Page<Transaction>> {
    val startDate = java.sql.Date(start.time)
    val endDate = java.sql.Date(end.time)
    // ...
}

// AFTER:
fun findByDateRange(
    @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) start: LocalDate,
    @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) end: LocalDate,
    // ...
): ResponseEntity<Page<Transaction>> {
    // Use start and end directly - no conversion needed
    return when (val result = standardizedTransactionService.findTransactionsByDateRangeStandardized(start, end, pageable)) {
        // ...
    }
}
```

**Key changes:**
- Replace `java.util.Date` with `LocalDate` in `@RequestParam`
- Use `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` for ISO-8601 date format
- Remove manual conversion from `java.util.Date` to `java.sql.Date`

#### Step 2.2: Update Service Layer

**Files to modify:**
- `StandardizedTransactionService.kt`
- `StandardizedPaymentService.kt`
- `StandardizedTransferService.kt`
- Any service method signatures that accept/return `java.sql.Date`

**Changes:**
- Update method signatures to use `LocalDate` instead of `Date`
- No business logic changes needed - just type updates

### Phase 3: Repository Layer Updates (Estimated: 1-2 hours)

**Files to modify:**
- `TransactionRepository.kt`
- `PaymentRepository.kt`
- `TransferRepository.kt`
- `MedicalExpenseRepository.kt`

**Example:**

```kotlin
// BEFORE:
interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByTransactionDateBetween(startDate: Date, endDate: Date): List<Transaction>
}

// AFTER:
interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun findByTransactionDateBetween(startDate: LocalDate, endDate: LocalDate): List<Transaction>
}
```

Spring Data JPA automatically handles `LocalDate` ↔ SQL `DATE` conversion via JDBC.

### Phase 4: Test Updates (Estimated: 3-4 hours)

#### Step 4.1: Update Test Data

**All test files** (unit, integration, functional) that create test data:

```groovy
// BEFORE (Groovy tests):
def transaction = new Transaction(
    // ...
    Date.valueOf("2024-01-15"),  // java.sql.Date
    // ...
)

// AFTER:
def transaction = new Transaction(
    // ...
    LocalDate.of(2024, 1, 15),
    // ...
)
```

**Alternative** (if using Groovy's flexible parsing):
```groovy
def transaction = new Transaction(
    // ...
    LocalDate.parse("2024-01-15"),
    // ...
)
```

#### Step 4.2: Critical Groovy Constructor Issue

**IMPORTANT:** Per CLAUDE.md, when calling Kotlin data classes from Groovy tests, **ALL constructor parameters must be provided**:

```groovy
// ✅ CORRECT:
def dto = new TransactionInputDto(
    null,                           // transactionId
    null,                           // guid
    1L,                            // accountId
    AccountType.Debit,             // accountType
    TransactionType.Expense,       // transactionType
    "checking_primary",            // accountNameOwner
    LocalDate.of(2024, 1, 15),     // transactionDate (UPDATED)
    "test description",            // description
    "test_category",               // category
    new BigDecimal("100.00"),      // amount
    TransactionState.Cleared,      // transactionState
    true,                          // activeStatus
    null,                          // reoccurringType
    null,                          // notes
    null,                          // dueDate
    null                           // receiptImageId
)

// ❌ INCORRECT (causes GroovyRuntimeException):
def dto = new TransactionInputDto(
    "checking_primary",
    LocalDate.of(2024, 1, 15),
    new BigDecimal("100.00")
)
```

#### Step 4.3: Test Files to Update

**Unit Tests:**
- `src/test/unit/groovy/finance/domain/TransactionSpec.groovy`
- `src/test/unit/groovy/finance/controllers/dto/*Spec.groovy`

**Integration Tests:**
- `src/test/integration/groovy/finance/repositories/*Spec.groovy`
- All specs that create transactions with dates

**Functional Tests:**
- `src/test/functional/groovy/finance/controllers/*Spec.groovy`

**Search command to find test files:**
```bash
grep -r "Date\.valueOf\|new Date(" src/test/ --include="*.groovy"
```

### Phase 5: GraphQL Layer (Optional/Informational)

**Note:** Per user request, not focusing on GraphQL, but for completeness:

**File:** `src/main/kotlin/finance/configurations/SqlDateScalar.kt`

```kotlin
// AFTER:
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object LocalDateScalar {
    val INSTANCE: GraphQLScalarType =
        GraphQLScalarType
            .newScalar()
            .name("Date")
            .description("A date scalar that handles java.time.LocalDate as String in YYYY-MM-DD format")
            .coercing(
                object : Coercing<LocalDate, String> {
                    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

                    override fun serialize(dataFetcherResult: Any): String =
                        when (dataFetcherResult) {
                            is LocalDate -> dataFetcherResult.format(formatter)
                            is String -> dataFetcherResult
                            else -> throw CoercingSerializeException("Unable to serialize $dataFetcherResult as LocalDate")
                        }

                    override fun parseValue(input: Any): LocalDate =
                        when (input) {
                            is String -> LocalDate.parse(input)
                            else -> throw CoercingParseValueException("Unable to parse $input as LocalDate")
                        }

                    override fun parseLiteral(input: Any): LocalDate =
                        when (input) {
                            is StringValue -> LocalDate.parse(input.value)
                            else -> throw CoercingParseLiteralException("Unable to parse literal $input as LocalDate")
                        }
                },
            ).build()
}
```

### Phase 6: Database Verification (Estimated: 1 hour)

**Objective:** Confirm database schema already uses `DATE` type (no migration needed).

**Verification steps:**

1. Connect to production database:
```sql
\d public.t_transaction
\d public.t_payment
\d public.t_transfer
\d public.t_pending_transaction
```

2. Verify column types:
```sql
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_name IN ('t_transaction', 't_payment', 't_transfer', 't_pending_transaction')
  AND column_name IN ('transaction_date', 'due_date');
```

Expected result: All should show `data_type = 'date'`.

**No Flyway migration needed** - the database schema is already correct. The bug is purely in the application layer.

### Phase 7: Testing & Validation (Estimated: 4-6 hours)

#### Test Strategy

**1. Timezone Variation Tests**

Run the application and tests in different timezones to ensure consistency:

```bash
# Test in CST (UTC-6)
TZ=America/Chicago ./gradlew test integrationTest functionalTest

# Test in UTC
TZ=UTC ./gradlew test integrationTest functionalTest

# Test in JST (UTC+9)
TZ=Asia/Tokyo ./gradlew test integrationTest functionalTest
```

**2. Edge Case Testing**

Create test cases for problematic scenarios:

```groovy
// Test: Create transaction at midnight local time
def "transaction created at midnight local should have correct date"() {
    given: "a transaction with date 2025-11-17"
    def transaction = new Transaction(
        // ... all params with:
        LocalDate.of(2025, 11, 17),
        // ...
    )

    when: "saving the transaction"
    def saved = transactionService.save(transaction)

    then: "date is preserved exactly"
    saved.transactionDate == LocalDate.of(2025, 11, 17)
    saved.transactionDate.toString() == "2025-11-17"
}

// Test: REST API endpoint
def "POST transaction with date via REST should preserve date"() {
    given: "transaction JSON with date"
    def json = '''
    {
        "accountNameOwner": "checking_primary",
        "transactionDate": "2025-11-17",
        "description": "test",
        "category": "test",
        "amount": 100.00,
        "accountType": "Debit",
        "transactionType": "Expense",
        "transactionState": "Cleared"
    }
    '''

    when: "posting to API"
    def response = restClient.post("/api/transaction", json)

    then: "response contains correct date"
    response.transactionDate == "2025-11-17"

    and: "database has correct date"
    def fromDb = transactionRepository.findByGuid(response.guid)
    fromDb.transactionDate.toString() == "2025-11-17"
}
```

**3. Date Range Query Tests**

```groovy
def "date range query should return transactions inclusively"() {
    given: "transactions on 2025-11-15, 2025-11-16, 2025-11-17"
    createTransaction(LocalDate.of(2025, 11, 15))
    createTransaction(LocalDate.of(2025, 11, 16))
    createTransaction(LocalDate.of(2025, 11, 17))

    when: "querying range 11-16 to 11-17"
    def results = transactionRepository.findByTransactionDateBetween(
        LocalDate.of(2025, 11, 16),
        LocalDate.of(2025, 11, 17)
    )

    then: "returns exactly 2 transactions"
    results.size() == 2
    results*.transactionDate.sort() == [
        LocalDate.of(2025, 11, 16),
        LocalDate.of(2025, 11, 17)
    ]
}
```

**4. Jackson Serialization Tests**

```kotlin
@Test
fun `LocalDate serializes to ISO-8601 date string`() {
    val transaction = Transaction(
        // ... with:
        transactionDate = LocalDate.of(2025, 11, 17)
    )

    val json = objectMapper.writeValueAsString(transaction)

    assertThat(json).contains("\"transactionDate\":\"2025-11-17\"")
}

@Test
fun `LocalDate deserializes from ISO-8601 date string`() {
    val json = """{"transactionDate":"2025-11-17", ...}"""

    val transaction = objectMapper.readValue(json, Transaction::class.java)

    assertThat(transaction.transactionDate).isEqualTo(LocalDate.of(2025, 11, 17))
}
```

#### Regression Testing

1. **Existing functional tests** should pass without modification (except date type changes)
2. **API contract tests** - verify REST responses still return dates as "YYYY-MM-DD" strings
3. **Database constraint tests** - ensure uniqueness constraints still work with LocalDate

#### Manual Testing Checklist

- [ ] Create transaction via REST API at 1:00 AM local time → verify correct date stored
- [ ] Create transaction via REST API at 11:59 PM local time → verify correct date stored
- [ ] Query transactions by date range → verify correct results
- [ ] Update transaction date via REST API → verify update succeeds
- [ ] Create payment with transaction_date → verify correct date
- [ ] Create transfer with transaction_date → verify correct date
- [ ] Frontend integration: verify UI displays correct dates after migration

## Risk Analysis

### Low Risk
- ✅ Database schema already uses `DATE` - no migration needed
- ✅ Jackson has built-in LocalDate support (ISO-8601)
- ✅ Spring Data JPA handles LocalDate natively
- ✅ JDBC 4.2+ maps LocalDate to SQL DATE automatically

### Medium Risk
- ⚠️ **Test updates** - Many test files need date type changes
- ⚠️ **Groovy interop** - Must provide all constructor parameters (per CLAUDE.md)
- ⚠️ **Third-party integrations** - If any external systems expect `java.sql.Date`, need adapter

### High Risk
- ❌ **None identified** - This is a straightforward type migration

## Rollback Plan

If issues arise during deployment:

1. **Code rollback:**
   - Git revert to commit before migration
   - Redeploy previous version

2. **Data safety:**
   - No database schema changes = no data migration needed
   - All dates remain as-is in the database
   - Rolling back code won't corrupt data

3. **Quick fix alternative:**
   - If LocalDate migration proves problematic, apply the minimal fix:
   - Replace `SimpleDateFormat.parse()` with `Date.valueOf()` in 3 files
   - This fixes the bug without changing types

## Timeline & Effort Estimate

| Phase | Estimated Time | Dependencies |
|-------|---------------|--------------|
| Phase 1: Domain Migration | 4-6 hours | None |
| Phase 2: REST API Updates | 2-3 hours | Phase 1 |
| Phase 3: Repository Updates | 1-2 hours | Phase 1 |
| Phase 4: Test Updates | 3-4 hours | Phases 1-3 |
| Phase 5: GraphQL (optional) | 1-2 hours | Phase 1 |
| Phase 6: DB Verification | 1 hour | None (parallel) |
| Phase 7: Testing & Validation | 4-6 hours | All phases |
| **Total** | **16-24 hours** | Sequential + Parallel |

**Recommended schedule:**
- **Day 1:** Phases 1-3 (domain, API, repository)
- **Day 2:** Phase 4 (test updates)
- **Day 3:** Phase 7 (validation & regression testing)

## Success Criteria

The fix is successful when:

1. ✅ Transaction created at 1:00 AM CST with date "2025-11-17" is stored as `2025-11-17` in database
2. ✅ Transaction created at 11:00 PM UTC with date "2025-11-17" is stored as `2025-11-17` in database
3. ✅ All existing tests pass (after updating test data types)
4. ✅ Manual testing confirms dates are preserved across timezones
5. ✅ REST API JSON responses still return dates as "YYYY-MM-DD" strings
6. ✅ Date range queries return correct results inclusively
7. ✅ No timezone-related errors in logs

## Post-Migration

### Documentation Updates

1. Update API documentation to reflect `LocalDate` usage
2. Add developer guidelines: "Always use `LocalDate` for date-only fields, never `java.sql.Date`"
3. Update CLAUDE.md with lesson learned

### Code Quality

1. Add CodeNarc rule to prevent future use of `java.sql.Date`:
```groovy
// .codenarc/rules.groovy
rule {
    name = 'NoSqlDateUsage'
    priority = 1
    description = 'Do not use java.sql.Date - use java.time.LocalDate instead'
}
```

2. Add pre-commit hook to check for `java.sql.Date` imports

### Monitoring

Add logging to confirm date handling:

```kotlin
logger.debug("Received transaction with date: ${dto.transactionDate} (type: ${dto.transactionDate::class.simpleName})")
logger.debug("Saved transaction with date: ${saved.transactionDate}")
```

Monitor for any date-related errors in first week post-deployment.

## Alternative Solution: Set JVM Timezone to UTC (Not Recommended)

**Approach:** Force the entire application to run in UTC timezone.

```bash
# In env.secrets or docker-compose
export TZ=UTC
# Or JVM arg:
-Duser.timezone=UTC
```

**Why this is NOT recommended:**
- ❌ Masks the problem instead of fixing it
- ❌ Doesn't address root cause (improper date type usage)
- ❌ Can introduce other issues (timestamp comparisons, logging timestamps)
- ❌ Brittle - depends on environment configuration
- ❌ Doesn't help if clients are in different timezones

**Only use if:** Migration to LocalDate is absolutely impossible due to external constraints.

## Conclusion

The transaction date off-by-one bug is caused by timezone-aware parsing of date-only strings using `SimpleDateFormat` and the legacy `java.sql.Date` API. The comprehensive solution is to migrate to `java.time.LocalDate`, which:

- Eliminates all timezone-related date bugs
- Modernizes the codebase
- Aligns with Java best practices
- Requires no database schema changes
- Has minimal risk due to strong type safety

**Recommended action:** Proceed with the full LocalDate migration (Phases 1-7) for a permanent, robust fix.

---

**Document Version:** 1.0
**Last Updated:** 2025-11-17
**Author:** Claude Code Analysis
