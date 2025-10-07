# Payment and Transaction Foreign Key Constraint Issues

## Document Purpose
This document captures two critical foreign key constraint issues discovered in production and outlines recommended solutions.

---

## Issue 1: Payment Insert with Invalid GUIDs (409 CONFLICT)

### Problem Description
When a client sends a payment creation request with pre-populated `guidSource` and `guidDestination` values that don't exist in the `t_transaction` table, the system attempts to save the payment directly, resulting in a foreign key constraint violation.

### Error Details
```
org.springframework.dao.DataIntegrityViolationException:
ERROR: insert or update on table "t_payment" violates foreign key constraint "fk_payment_guid_source"
Detail: Key (guid_source)=(99c43f04-2fe6-482e-9d68-67b62d955b35) is not present in table "t_transaction"
```

### Request Example
```json
{
  "amount": 0.01,
  "transactionDate": "2025-10-07T10:43:47.179Z",
  "sourceAccount": "bcu-checking_brian",
  "destinationAccount": "amex_brian",
  "guidSource": "99c43f04-2fe6-482e-9d68-67b62d955b35",
  "guidDestination": "1c368a73-2392-4397-88eb-9e9109d365d3",
  "activeStatus": true
}
```

### Root Cause
The `StandardizedPaymentService.save()` method checks if GUIDs are null/blank:
```kotlin
if (entity.guidSource.isNullOrBlank() || entity.guidDestination.isNullOrBlank()) {
    // Create transactions
}
```

When GUIDs are provided (not null/blank), the method **assumes they are valid** and skips transaction creation, leading to the foreign key violation.

### Impact
- HTTP 409 CONFLICT responses to users
- Failed payment creation in production
- Poor user experience when retrying failed requests

### Recommended Solutions

#### Option 1: Always Create Transactions (Ignore Client GUIDs)
**Pros:**
- Simple implementation
- Guarantees valid transactions
- No validation overhead

**Cons:**
- Ignores client-provided GUIDs (may break API contract)
- Not idempotent for retry scenarios

```kotlin
override fun save(entity: Payment): ServiceResult<Payment> =
    handleServiceOperation("save", entity.paymentId) {
        // Validation...

        // Always create transactions, ignore any provided GUIDs
        logger.info("Creating transactions for payment: ${entity.sourceAccount} -> ${entity.destinationAccount}")

        // Process accounts
        processPaymentAccount(entity.destinationAccount)
        processPaymentAccount(entity.sourceAccount)

        // Create transactions and set GUIDs
        // ... (existing transaction creation logic)

        paymentRepository.saveAndFlush(entity)
    }
```

#### Option 2: Validate GUIDs Before Saving
**Pros:**
- Respects client-provided GUIDs when valid
- Clear error messages for invalid GUIDs
- More flexible API behavior

**Cons:**
- Additional database queries for validation
- More complex logic

```kotlin
override fun save(entity: Payment): ServiceResult<Payment> =
    handleServiceOperation("save", entity.paymentId) {
        // Validation...

        // If GUIDs are provided, validate they exist
        if (!entity.guidSource.isNullOrBlank() && !entity.guidDestination.isNullOrBlank()) {
            // Validate GUIDs exist in t_transaction
            val sourceExists = transactionRepository.findByGuid(entity.guidSource!!).isPresent
            val destExists = transactionRepository.findByGuid(entity.guidDestination!!).isPresent

            if (!sourceExists || !destExists) {
                throw ValidationException(
                    "Provided transaction GUIDs do not exist. " +
                    "Remove guidSource and guidDestination to auto-create transactions."
                )
            }
        } else {
            // Create transactions if GUIDs not provided or blank
            logger.info("Creating transactions for payment: ${entity.sourceAccount} -> ${entity.destinationAccount}")
            // ... (existing transaction creation logic)
        }

        paymentRepository.saveAndFlush(entity)
    }
```

#### Option 3: Input DTO Validation (Recommended)
**Pros:**
- Prevents invalid requests at API boundary
- Clear separation of concerns
- Best practice for REST APIs

**Cons:**
- Requires DTO layer updates
- Changes API contract

```kotlin
// In PaymentController
@PostMapping(consumes = ["application/json"], produces = ["application/json"])
override fun save(@Valid @RequestBody entity: Payment): ResponseEntity<Payment> {
    // Strip any client-provided GUIDs before processing
    entity.guidSource = null
    entity.guidDestination = null

    when (val result = standardizedPaymentService.save(entity)) {
        // ... handle result
    }
}
```

---

## Issue 2: Transaction Delete with Referencing Payments (500 ERROR)

### Problem Description
When attempting to delete a transaction that is referenced by a payment record (via `fk_payment_guid_destination` or `fk_payment_guid_source`), the database prevents the deletion due to foreign key constraints, resulting in a 500 Internal Server Error.

### Error Details
```
org.springframework.dao.DataIntegrityViolationException:
ERROR: update or delete on table "t_transaction" violates foreign key constraint
"fk_payment_guid_destination" on table "t_payment"
Detail: Key (guid)=(ae5a4362-aea7-4964-b9dd-42922cf7ed43) is still referenced from table "t_payment"
```

### Root Cause
The database enforces referential integrity, preventing deletion of transactions that are referenced by payment records. The application doesn't check for these references before attempting deletion.

### Impact
- HTTP 500 INTERNAL_SERVER_ERROR responses
- Failed transaction deletions in production
- User confusion when delete operations fail silently
- Potential data inconsistency if transactions are deleted without cleaning up payments

### Recommended Solutions

#### Option 1: Cascade Delete Payments
**Pros:**
- Automatic cleanup of related data
- Maintains referential integrity
- Simple implementation

**Cons:**
- May delete more data than intended
- Users might not expect payments to be deleted

```kotlin
// In StandardizedTransactionService
override fun deleteById(id: String): ServiceResult<Boolean> =
    handleServiceOperation("deleteById", id) {
        val optionalTransaction = transactionRepository.findByGuid(id)
        if (optionalTransaction.isEmpty) {
            throw jakarta.persistence.EntityNotFoundException("Transaction not found: $id")
        }

        val transaction = optionalTransaction.get()

        // Find and delete all payments referencing this transaction
        val referencingPayments = paymentRepository.findByGuidSourceOrGuidDestination(
            transaction.guid,
            transaction.guid
        )

        if (referencingPayments.isNotEmpty()) {
            logger.info("Deleting ${referencingPayments.size} payments referencing transaction: ${transaction.guid}")
            paymentRepository.deleteAll(referencingPayments)
        }

        transactionRepository.delete(transaction)
        true
    }
```

#### Option 2: Block Delete with Informative Error (Recommended)
**Pros:**
- Prevents accidental data loss
- Clear user feedback
- Maintains data integrity

**Cons:**
- Users must manually delete payments first
- Extra step in workflow

```kotlin
override fun deleteById(id: String): ServiceResult<Boolean> =
    handleServiceOperation("deleteById", id) {
        val optionalTransaction = transactionRepository.findByGuid(id)
        if (optionalTransaction.isEmpty) {
            throw jakarta.persistence.EntityNotFoundException("Transaction not found: $id")
        }

        val transaction = optionalTransaction.get()

        // Check for referencing payments
        val referencingPayments = paymentRepository.findByGuidSourceOrGuidDestination(
            transaction.guid,
            transaction.guid
        )

        if (referencingPayments.isNotEmpty()) {
            throw BusinessLogicException(
                "Cannot delete transaction ${transaction.guid} because it is referenced by " +
                "${referencingPayments.size} payment(s). Please delete the related payments first."
            )
        }

        transactionRepository.delete(transaction)
        true
    }
```

#### Option 3: Database-Level Cascade (Schema Change)
**Pros:**
- Enforced at database level
- Consistent across all access methods
- No application code changes needed

**Cons:**
- Requires database migration
- May affect other parts of the system
- Harder to customize behavior

```sql
-- Flyway migration to add CASCADE
ALTER TABLE t_payment
DROP CONSTRAINT fk_payment_guid_source,
ADD CONSTRAINT fk_payment_guid_source
    FOREIGN KEY (guid_source)
    REFERENCES t_transaction(guid)
    ON DELETE CASCADE;

ALTER TABLE t_payment
DROP CONSTRAINT fk_payment_guid_destination,
ADD CONSTRAINT fk_payment_guid_destination
    FOREIGN KEY (guid_destination)
    REFERENCES t_transaction(guid)
    ON DELETE CASCADE;
```

---

## Repository Method Additions Required

### For Solution Implementation

```kotlin
// In PaymentRepository.kt
interface PaymentRepository : JpaRepository<Payment, Long> {
    // Existing methods...

    // Find payments referencing a specific transaction GUID
    fun findByGuidSourceOrGuidDestination(guidSource: String, guidDestination: String): List<Payment>

    // Alternative query methods
    @Query("SELECT p FROM Payment p WHERE p.guidSource = :guid OR p.guidDestination = :guid")
    fun findByTransactionGuid(@Param("guid") guid: String): List<Payment>
}
```

---

## Testing Requirements

### Issue 1: Payment with Invalid GUIDs
- [ ] Unit test: `save()` with null GUIDs creates transactions
- [ ] Unit test: `save()` with valid GUIDs uses existing transactions
- [ ] Unit test: `save()` with invalid GUIDs throws ValidationException
- [ ] Functional test: POST /api/payment without GUIDs succeeds
- [ ] Functional test: POST /api/payment with invalid GUIDs returns 400 BAD_REQUEST
- [ ] Functional test: POST /api/payment with valid GUIDs succeeds

### Issue 2: Transaction Delete with References
- [ ] Unit test: `deleteById()` succeeds when no payment references exist
- [ ] Unit test: `deleteById()` throws exception when payment references exist
- [ ] Functional test: DELETE transaction without payment references succeeds
- [ ] Functional test: DELETE transaction with payment references returns appropriate error
- [ ] Integration test: Cascade delete behavior (if implemented)

---

## Implementation Priority

### Critical (Immediate)
1. **Issue 1 - Option 3**: Strip client GUIDs at controller level (quick fix)
2. **Issue 2 - Option 2**: Block delete with informative error (prevents data loss)

### Important (Next Sprint)
1. Add repository methods for finding referencing payments
2. Comprehensive unit and functional tests
3. API documentation updates

### Enhancement (Future)
1. Consider DTO-based validation for all payment endpoints
2. Database schema review for cascade delete appropriateness
3. Bulk payment deletion API for better UX

---

## Notes

- Both issues stem from foreign key constraints on the `t_payment` table
- Solutions should prioritize data integrity and user experience
- Consider transaction isolation levels when implementing cascading operations
- Monitor production logs for similar patterns with Transfer entities
- Update API documentation to clarify GUID handling behavior

## Related Files

- `StandardizedPaymentService.kt:49-114` - save() method
- `StandardizedTransactionService.kt:110-120` - deleteById() method
- `PaymentController.kt:197-222` - POST /api/payment endpoint
- `TransactionController.kt` - DELETE endpoint
- `Payment.kt:72-77` - GUID field definitions with foreign key constraints

---

## Implementation Status

### Issue 2: Transaction Delete with Referencing Payments ✅ IMPLEMENTED

**Implementation Date**: 2025-10-07

**Solution Chosen**: Option 2 - Block Delete with Informative Error

**Changes Made**:
1. Added `findByGuidSourceOrGuidDestination()` method to `PaymentRepository.kt`
2. Updated `StandardizedTransactionService` constructor to include `PaymentRepository`
3. Modified `deleteById()` method to check for payment references before deletion
4. Created comprehensive TDD tests covering all scenarios
5. All tests passing (4/4 deleteById test scenarios)

**Error Message Format**:
```
Cannot delete transaction {guid} because it is referenced by {count} payment(s).
Please delete the related payments first.
```

**Files Modified**:
- `PaymentRepository.kt`: Added findByGuidSourceOrGuidDestination() method
- `StandardizedTransactionService.kt`: Added payment reference check in deleteById()
- `StandardizedTransactionServiceSpec.groovy`: Added TDD tests for payment reference scenarios

**HTTP Response**:
- Returns `ServiceResult.BusinessError` which translates to appropriate HTTP status
- Clear, user-friendly error message
- No 500 errors - proper error handling

---

**Last Updated**: 2025-10-07
**Status**:
- **Issue 1**: Documented, Awaiting Implementation
- **Issue 2**: ✅ IMPLEMENTED and TESTED
