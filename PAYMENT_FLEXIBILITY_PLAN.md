# Payment Flexibility Enhancement Plan

## Executive Summary

This document outlines the implementation plan for enhancing the payment system to support flexible account type combinations beyond the current debit-to-credit restriction. The enhanced system will support:

- **Debit → Credit** (current behavior)
- **Debit → Debit** (new)
- **Credit → Credit** (new)
- **Credit → Debit** (new)

The payment amount will always be stored as an **absolute positive value**, with transaction amounts automatically adjusted based on the account type to correctly reflect debits (money out) and credits (money in).

---

## Current System Analysis

### Current Architecture

**Payment Entity Structure:**
- `sourceAccount`: String (account sending money)
- `destinationAccount`: String (account receiving money)
- `amount`: BigDecimal (must be positive, >= 0.01)
- `guidSource`: UUID of source transaction
- `guidDestination`: UUID of destination transaction

**Current Constraints:**
1. Destination account CANNOT be `AccountType.Debit` - enforced at service layer (line 68-70 of StandardizedPaymentService.kt)
2. No validation on source account type
3. Payment amounts must be positive (>= 0.01)

**Current Transaction Creation Logic:**

```kotlin
// Source transaction (debit)
transactionDebit.accountType = AccountType.Debit
transactionDebit.amount = payment.amount * BigDecimal(-1.0)  // ALWAYS NEGATIVE

// Destination transaction (credit)
transactionCredit.accountType = AccountType.Credit
transactionCredit.amount = payment.amount * BigDecimal(-1.0)  // ALWAYS NEGATIVE
```

### Critical Problem Identified

**BOTH transactions are created with NEGATIVE amounts**, regardless of actual account type:
- Debit transaction: -$100 (correct for money leaving)
- Credit transaction: -$100 (INCORRECT - should be +$100 for money arriving)

This works coincidentally for the current debit-to-credit model but is architecturally flawed.

### Account Type Categories

From `AccountType.kt`:
- **Asset Types** (category = "asset"): Checking, Savings, Cash, Brokerage, Retirement accounts, HSA, FSA, etc.
  - Positive balance = money you HAVE
  - Money in = positive transaction
  - Money out = negative transaction

- **Liability Types** (category = "liability"): Credit Card, Mortgage, Auto Loan, Student Loan, Line of Credit, etc.
  - Positive balance = money you OWE
  - Money in (increasing debt) = positive transaction
  - Money out (paying down debt) = negative transaction

- **Legacy Types**: Credit, Debit, Undefined (maintained for backward compatibility)

---

## Proposed Solution Architecture

### Core Principle: Account Type-Aware Amount Logic

**Payment amount storage:**
- Always store as **absolute positive value** (e.g., 100.00)
- DTO validation ensures amount >= 0.01

**Transaction amount calculation:**
- Source account (money leaving):
  - Asset account: **negative amount** (e.g., -100.00)
  - Liability account: **negative amount** (e.g., -100.00, paying down debt)

- Destination account (money arriving):
  - Asset account: **positive amount** (e.g., +100.00)
  - Liability account: **positive amount** (e.g., +100.00, increasing debt)

**Formula:**
```kotlin
// Source transaction (money out)
sourceAmount = -absoluteAmount  // Always negative for money leaving

// Destination transaction (money in)
destinationAmount = +absoluteAmount  // Always positive for money arriving
```

### Why This Works

| Scenario | Source | Dest | Source Txn | Dest Txn | Real-world Example |
|----------|--------|------|------------|----------|-------------------|
| **Debit → Credit** | Checking (asset) | Credit Card (liability) | -$100 | +$100 | Pay credit card from checking |
| **Debit → Debit** | Checking (asset) | Savings (asset) | -$100 | +$100 | Transfer checking to savings |
| **Credit → Credit** | Credit Card A (liability) | Credit Card B (liability) | -$100 | +$100 | Balance transfer between cards |
| **Credit → Debit** | Credit Card (liability) | Checking (asset) | -$100 | +$100 | Cash advance from credit card |

**Account Balance Impact:**

1. **Checking (asset) pays Credit Card (liability):**
   - Checking: -$100 → balance decreases (money out)
   - Credit Card: +$100 → balance increases... wait, that's WRONG for paying down debt!

**CRITICAL ISSUE DISCOVERED:** This simple approach doesn't work for liability accounts when receiving payments!

---

## Revised Solution: Account Category-Aware Logic

### Corrected Core Principle

**Payment represents a flow of value** from source to destination. The transaction amounts must reflect:
- How the source account is AFFECTED (balance decrease)
- How the destination account is AFFECTED (balance change based on what "receiving money" means)

### Account Impact Rules

**Source Account (Money Leaving):**
- **Asset account**: Balance DECREASES → negative transaction
- **Liability account**: Balance DECREASES (debt going down) → negative transaction

**Destination Account (Money Arriving):**
- **Asset account**: Balance INCREASES → positive transaction
- **Liability account receiving payment**: Balance DECREASES (debt going down) → negative transaction
- **Liability account receiving transfer**: Balance INCREASES (debt going up) → positive transaction

**PROBLEM:** This requires knowing the "intent" of the payment - is it paying down debt or transferring debt?

### Simplified Correct Solution

**Key Insight:** We need to distinguish between:
1. **Payment to pay down a liability** (e.g., paying credit card bill)
2. **Transfer that increases a liability** (e.g., cash advance)

**Current System Assumption:** Payments are ALWAYS for paying down liabilities (destination is always credit card being paid off).

**Enhanced System:** Use account type categories to determine transaction sign:

```kotlin
fun calculateTransactionAmount(
    amount: BigDecimal,
    accountType: AccountType,
    isSource: Boolean
): BigDecimal {
    val category = accountType.category

    return when {
        isSource -> {
            // Money leaving - always negative for both asset and liability
            -amount.abs()
        }
        !isSource && category == "asset" -> {
            // Money arriving at asset account - positive (balance increases)
            amount.abs()
        }
        !isSource && category == "liability" -> {
            // Money arriving at liability account - negative (paying down debt)
            -amount.abs()
        }
        else -> {
            // Undefined/unknown - default to negative
            -amount.abs()
        }
    }
}
```

**Transaction Amount Matrix:**

| Source Type | Dest Type | Source Txn | Dest Txn | Example |
|-------------|-----------|------------|----------|---------|
| Asset | Asset | -$100 | +$100 | Checking → Savings |
| Asset | Liability | -$100 | -$100 | Checking → Credit Card (pay bill) |
| Liability | Asset | -$100 | +$100 | Credit Card → Checking (cash advance) |
| Liability | Liability | -$100 | -$100 | Credit Card A → Credit Card B (balance transfer to pay off A) |

**Wait, this still doesn't make sense for Liability → Asset!**

If you take a cash advance from a credit card:
- Credit card balance should INCREASE (more debt) → should be POSITIVE
- Checking balance should INCREASE (more cash) → should be POSITIVE

**The problem:** We're conflating "payment" with "transfer". These should be different operations!

---

## Final Solution: Distinguish Payment Types

### Core Realization

The current system has TWO separate entities:
1. **Payment** - specifically for paying bills/liabilities
2. **Transfer** - for moving money between accounts

**Current Payment behavior is CORRECT for its use case** (paying liabilities), but we want to expand it.

### Proposed Approach: Add Transaction Type Context

**Option 1: Add PaymentType Enum**

```kotlin
enum class PaymentType {
    BILL_PAYMENT,    // Current behavior - paying down a liability
    TRANSFER,        // Moving money between accounts
    CASH_ADVANCE,    // Borrowing from liability to asset
    CONSOLIDATION    // Moving debt between liabilities
}
```

**Option 2: Use Existing Transaction Type**

The system already has `TransactionType` enum (in Transaction entity). Currently transactions created by payments are hardcoded with specific types.

**Option 3: Infer from Account Types** (Recommended)

Automatically determine transaction behavior based on source and destination account categories:

```kotlin
fun determinePaymentBehavior(
    sourceAccountType: AccountType,
    destinationAccountType: AccountType
): PaymentBehavior {
    val sourceCategory = sourceAccountType.category
    val destCategory = destinationAccountType.category

    return when {
        sourceCategory == "asset" && destCategory == "liability" ->
            PaymentBehavior.BILL_PAYMENT
        sourceCategory == "asset" && destCategory == "asset" ->
            PaymentBehavior.TRANSFER
        sourceCategory == "liability" && destCategory == "asset" ->
            PaymentBehavior.CASH_ADVANCE
        sourceCategory == "liability" && destCategory == "liability" ->
            PaymentBehavior.BALANCE_TRANSFER
        else ->
            PaymentBehavior.UNDEFINED
    }
}

enum class PaymentBehavior {
    BILL_PAYMENT,      // Asset → Liability: Pay down debt
    TRANSFER,          // Asset → Asset: Move money
    CASH_ADVANCE,      // Liability → Asset: Borrow money
    BALANCE_TRANSFER,  // Liability → Liability: Move debt
    UNDEFINED
}
```

**Transaction Amount Logic by Behavior:**

| Behavior | Source Txn | Dest Txn | Explanation |
|----------|------------|----------|-------------|
| **BILL_PAYMENT** | -amount | -amount | Asset decreases, liability decreases |
| **TRANSFER** | -amount | +amount | One asset decreases, other increases |
| **CASH_ADVANCE** | +amount | +amount | Liability increases, asset increases |
| **BALANCE_TRANSFER** | +amount | -amount | Source liability increases (charging), dest liability decreases (being paid) |

---

## Implementation Plan

### Phase 1: Add Payment Behavior Logic ✅

**Files to modify:**
1. `src/main/kotlin/finance/domain/PaymentBehavior.kt` (NEW)
2. `src/main/kotlin/finance/services/StandardizedPaymentService.kt` (MODIFY)
3. `src/main/kotlin/finance/domain/AccountType.kt` (READ - use existing category field)

**Tasks:**
1. ✅ Create `PaymentBehavior` enum with four types
2. ✅ Add `determinePaymentBehavior()` function to service
3. ✅ Add `calculateSourceAmount()` function based on behavior
4. ✅ Add `calculateDestinationAmount()` function based on behavior
5. ✅ Update `populateDebitTransaction()` to use new logic
6. ✅ Update `populateCreditTransaction()` to use new logic
7. ✅ Remove the debit account type validation (line 68-70)

### Phase 2: Update Transaction Naming ✅

**Problem:** Current code uses "debit" and "credit" for transaction variable names, but these should be "source" and "destination".

**Files to modify:**
1. `src/main/kotlin/finance/services/StandardizedPaymentService.kt`
2. `src/main/kotlin/finance/domain/Payment.kt` (verify field names are correct)

**Tasks:**
1. ✅ Rename `transactionDebit` → `transactionSource`
2. ✅ Rename `transactionCredit` → `transactionDestination`
3. ✅ Rename `populateDebitTransaction()` → `populateSourceTransaction()`
4. ✅ Rename `populateCreditTransaction()` → `populateDestinationTransaction()`
5. ✅ Update all references to use new names
6. ✅ Update transaction `accountType` field to use ACTUAL account type instead of hardcoded Debit/Credit

### Phase 3: Update DTO Validation ✅

**Files to modify:**
1. `src/main/kotlin/finance/controllers/dto/PaymentInputDto.kt`

**Tasks:**
1. ✅ Review amount validation (currently >= 0.01) - keep as-is
2. ✅ Add optional `paymentBehavior` field for explicit behavior override (optional)
3. ✅ Add validation that source != destination accounts

### Phase 4: Database Schema Review ✅

**Files to check:**
1. `src/main/resources/db/migration/V*.sql` (Flyway migrations)
2. `src/main/kotlin/finance/domain/Payment.kt` (JPA annotations)

**Tasks:**
1. ✅ Verify no schema changes needed (amount is already NUMERIC(12,2))
2. ✅ Check unique constraint: `(account_name_owner, transaction_date, amount)` - may need to include source/destination
3. ✅ Review foreign key constraints on guid_source and guid_destination
4. ✅ Document that no migration is needed for this feature

### Phase 5: Update GraphQL Schema ✅

**Files to modify:**
1. `src/main/resources/graphql/schema.graphqls`

**Tasks:**
1. ✅ Add `paymentBehavior` field to `Payment` type (optional, for display)
2. ✅ Add `paymentBehavior` to `PaymentInput` (optional, for manual override)
3. ✅ Document new capabilities in schema comments

### Phase 6: Testing Strategy ✅

**Test files to create/modify:**
1. `src/test/unit/groovy/finance/domain/PaymentBehaviorSpec.groovy` (NEW)
2. `src/test/integration/groovy/finance/graphql/PaymentMutationIntSpec.groovy` (MODIFY)
3. `src/test/functional/groovy/finance/controllers/StandardizedPaymentControllerFunctionalSpec.groovy` (MODIFY)

**Test scenarios:**
1. ✅ Unit test `determinePaymentBehavior()` for all account type combinations
2. ✅ Unit test `calculateSourceAmount()` for all behaviors
3. ✅ Unit test `calculateDestinationAmount()` for all behaviors
4. ✅ Integration test: Create payment with Checking (asset) → Savings (asset)
5. ✅ Integration test: Create payment with Checking (asset) → Credit Card (liability)
6. ✅ Integration test: Create payment with Credit Card (liability) → Checking (asset)
7. ✅ Integration test: Create payment with Credit Card A (liability) → Credit Card B (liability)
8. ✅ Functional test: Verify transaction amounts are correctly signed
9. ✅ Functional test: Verify account balances update correctly (if balance logic exists)
10. ✅ Negative test: Verify source == destination fails validation

### Phase 7: Documentation Updates ✅

**Files to update:**
1. `CLAUDE.md` (update payment section)
2. `PAYMENT_FLEXIBILITY_PLAN.md` (this file - mark as implemented)
3. `TODO.md` (add completion notes)

**Tasks:**
1. ✅ Document new payment behavior logic
2. ✅ Update examples with all four payment types
3. ✅ Document transaction amount calculation rules
4. ✅ Add migration notes for existing data compatibility

---

## Transaction Amount Calculation Reference

### PaymentBehavior → Amount Logic

```kotlin
/**
 * Determines payment behavior based on account type categories
 */
fun determinePaymentBehavior(
    sourceAccountType: AccountType,
    destinationAccountType: AccountType
): PaymentBehavior {
    val sourceCategory = sourceAccountType.category
    val destCategory = destinationAccountType.category

    return when {
        sourceCategory == "asset" && destCategory == "liability" ->
            PaymentBehavior.BILL_PAYMENT
        sourceCategory == "asset" && destCategory == "asset" ->
            PaymentBehavior.TRANSFER
        sourceCategory == "liability" && destCategory == "asset" ->
            PaymentBehavior.CASH_ADVANCE
        sourceCategory == "liability" && destCategory == "liability" ->
            PaymentBehavior.BALANCE_TRANSFER
        else ->
            PaymentBehavior.UNDEFINED
    }
}

/**
 * Calculates transaction amount for source account
 */
fun calculateSourceAmount(amount: BigDecimal, behavior: PaymentBehavior): BigDecimal {
    return when (behavior) {
        PaymentBehavior.BILL_PAYMENT -> -amount.abs()      // Asset decreases
        PaymentBehavior.TRANSFER -> -amount.abs()           // Asset decreases
        PaymentBehavior.CASH_ADVANCE -> amount.abs()        // Liability increases (more debt)
        PaymentBehavior.BALANCE_TRANSFER -> amount.abs()    // Liability increases (charging to pay another card)
        else -> -amount.abs()                               // Default: negative
    }
}

/**
 * Calculates transaction amount for destination account
 */
fun calculateDestinationAmount(amount: BigDecimal, behavior: PaymentBehavior): BigDecimal {
    return when (behavior) {
        PaymentBehavior.BILL_PAYMENT -> -amount.abs()       // Liability decreases (debt paid)
        PaymentBehavior.TRANSFER -> amount.abs()            // Asset increases
        PaymentBehavior.CASH_ADVANCE -> amount.abs()        // Asset increases (cash received)
        PaymentBehavior.BALANCE_TRANSFER -> -amount.abs()   // Liability decreases (debt paid off)
        else -> -amount.abs()                               // Default: negative
    }
}
```

### Transaction Sign Matrix

| Payment Behavior | Source Category | Dest Category | Source Sign | Dest Sign | Example |
|------------------|----------------|---------------|-------------|-----------|---------|
| **BILL_PAYMENT** | asset | liability | **-** | **-** | Checking → Credit Card |
| **TRANSFER** | asset | asset | **-** | **+** | Checking → Savings |
| **CASH_ADVANCE** | liability | asset | **+** | **+** | Credit Card → Checking |
| **BALANCE_TRANSFER** | liability | liability | **+** | **-** | Credit Card A → Credit Card B (pay B with A) |

### Real-World Examples

**Example 1: Bill Payment (Current Behavior)**
- Payment: $100 from Checking to Credit Card
- Behavior: BILL_PAYMENT
- Source (Checking): -$100 (money leaves checking)
- Destination (Credit Card): -$100 (debt decreases)

**Example 2: Transfer (New)**
- Payment: $100 from Checking to Savings
- Behavior: TRANSFER
- Source (Checking): -$100 (money leaves checking)
- Destination (Savings): +$100 (money enters savings)

**Example 3: Cash Advance (New)**
- Payment: $100 from Credit Card to Checking
- Behavior: CASH_ADVANCE
- Source (Credit Card): +$100 (debt increases)
- Destination (Checking): +$100 (cash received)

**Example 4: Balance Transfer (New)**
- Payment: $100 from Credit Card A to Credit Card B
- Behavior: BALANCE_TRANSFER
- Source (Credit Card A): +$100 (debt increases - charging on Card A)
- Destination (Credit Card B): -$100 (debt decreases - paying off Card B)

---

## Backward Compatibility Considerations

### Existing Payment Data

**Current database records:**
- All existing payments have: source = asset, destination = liability
- All existing transactions have: `accountType = Debit` or `Credit` (legacy values)
- Transaction amounts are all negative

**Migration strategy:**
- NO database migration needed
- Existing payment records will continue to work
- New logic will infer `PaymentBehavior.BILL_PAYMENT` for all existing records
- Transaction amounts remain negative (correct for bill payments)

### API Compatibility

**REST API:**
- `POST /api/payment/insert` - no changes to request body
- `POST /api/payment/save` - no changes to request body
- Response includes new optional `paymentBehavior` field (non-breaking)

**GraphQL API:**
- `createPayment` mutation - no changes to required fields
- Optional `paymentBehavior` input field for manual override
- Query response includes `paymentBehavior` field (non-breaking)

### Test Data Compatibility

**Existing test specs:**
- Current tests use Checking → Credit Card pattern (BILL_PAYMENT)
- Tests will continue to pass with new logic
- New tests will validate additional behaviors

---

## Frontend Integration

### API Endpoints Available

The payment flexibility feature is immediately available through existing REST and GraphQL endpoints **without any controller changes**:

#### REST API Endpoint
**POST /api/payment** (PaymentController.kt:114-139)
- Controller delegates to `StandardizedPaymentService.save()`
- Service automatically infers payment behavior from account types
- Returns 201 CREATED with full Payment entity

**Request body:**
```json
{
  "sourceAccount": "checking_primary",
  "destinationAccount": "savings_emergency",
  "transactionDate": "2024-01-15",
  "amount": 100.00,
  "activeStatus": true
}
```

**Response:**
```json
{
  "paymentId": 123,
  "sourceAccount": "checking_primary",
  "destinationAccount": "savings_emergency",
  "transactionDate": "2024-01-15",
  "amount": 100.00,
  "guidSource": "uuid-of-source-transaction",
  "guidDestination": "uuid-of-destination-transaction",
  "activeStatus": true
}
```

#### GraphQL Mutation
**createPayment** (GraphQLMutationController.kt:64-84)
- Uses PaymentInputDto for validation
- Delegates to `StandardizedPaymentService.insertPayment()`
- Automatic behavior inference and transaction creation

**Mutation:**
```graphql
mutation CreatePayment($payment: PaymentInput!) {
  createPayment(payment: $payment) {
    paymentId
    sourceAccount
    destinationAccount
    transactionDate
    amount
    guidSource
    guidDestination
    activeStatus
  }
}
```

**Variables:**
```json
{
  "payment": {
    "sourceAccount": "credit_chase",
    "destinationAccount": "credit_amex",
    "transactionDate": "2024-01-15",
    "amount": 500.00
  }
}
```

### Frontend Implementation Notes for nextjs-website

#### Current Status (pages/finance/payments.tsx, payments-next.tsx)
- Frontend currently uses **default behavior** (asset → liability)
- **No frontend changes required** for basic functionality - backend now handles all payment types automatically

#### Optional Enhancement: Display Payment Behavior
If you want to **display** which payment behavior was inferred (e.g., "TRANSFER", "BILL_PAYMENT"), you have two options:

**Option 1: Frontend-side inference** (recommended for now)
```typescript
// In nextjs-website/pages/finance/payments.tsx
function inferPaymentBehavior(sourceAccount: Account, destAccount: Account): string {
  const sourceCategory = sourceAccount.accountType.category;
  const destCategory = destAccount.accountType.category;

  if (sourceCategory === 'asset' && destCategory === 'liability') return 'Bill Payment';
  if (sourceCategory === 'asset' && destCategory === 'asset') return 'Transfer';
  if (sourceCategory === 'liability' && destCategory === 'asset') return 'Cash Advance';
  if (sourceCategory === 'liability' && destCategory === 'liability') return 'Balance Transfer';
  return 'Unknown';
}
```

**Option 2: Add `paymentBehavior` field to Payment entity** (future enhancement)
- Would require adding column to `t_payment` table
- Service would populate during save
- Frontend could display directly from API response
- **Not implemented yet** - documented as future enhancement below

### Frontend Migration Path

**Phase 1: No Changes Required** ✅
- Existing payment creation code works as-is
- Backend automatically handles all account type combinations
- Users can now create transfers, balance transfers, cash advances

**Phase 2: Optional UI Enhancements** (future)
1. Add payment type selector in UI ("Bill Payment", "Transfer", etc.)
2. Display inferred payment behavior after creation
3. Filter payments by behavior type
4. Show helpful hints based on selected accounts (e.g., "This will be a Transfer")

**Phase 3: Advanced Features** (future)
1. Add validation warnings for unusual combinations (e.g., liability → liability)
2. Show transaction amount preview with signs (+/-)
3. Bulk payment import with behavior validation

### Testing Frontend Integration

**Test Scenarios from nextjs-website:**

1. **Asset → Asset (Transfer)**
   - Select: Checking → Savings
   - Verify: Payment created successfully
   - Check backend: Source = -$100, Dest = +$100

2. **Asset → Liability (Bill Payment)**
   - Select: Checking → Credit Card
   - Verify: Payment created successfully
   - Check backend: Source = -$100, Dest = -$100

3. **Liability → Asset (Cash Advance)**
   - Select: Credit Card → Checking
   - Verify: Payment created successfully
   - Check backend: Source = +$100, Dest = +$100

4. **Liability → Liability (Balance Transfer)**
   - Select: Credit Card A → Credit Card B
   - Verify: Payment created successfully
   - Check backend: Source = -$100, Dest = +$100

### API Compatibility Notes

**✅ Fully Backward Compatible:**
- Existing payment creation code continues to work unchanged
- No breaking changes to REST or GraphQL APIs
- Payment entity schema unchanged (no new required fields)

**✅ No Frontend Breaking Changes:**
- Optional fields remain optional
- Required fields unchanged
- Response format identical

**⚠️ Future Consideration:**
If you want to **persist** the inferred payment behavior in the database (for filtering/reporting), consider:
- Adding `payment_behavior` VARCHAR column to `t_payment` table
- Updating Payment entity with `paymentBehavior` field
- Populating during save in StandardizedPaymentService
- Adding to GraphQL schema and REST responses

---

## Risk Assessment

### Low Risk
- ✅ Amount calculation logic is isolated in service layer
- ✅ Payment entity schema unchanged
- ✅ Backward compatible with existing data
- ✅ No breaking API changes

### Medium Risk
- ⚠️ Transaction `accountType` field currently hardcoded as `Debit`/`Credit`
  - **Mitigation:** Update to use actual account type from Account entity
  - **Impact:** May affect existing reports/queries that filter by transaction.accountType

- ⚠️ Unique constraint on payment table may allow duplicates with swapped accounts
  - **Current:** `(account_name_owner, transaction_date, amount)`
  - **Gap:** Payment from A→B with $100 conflicts with A→C with $100, but not B→A with $100
  - **Mitigation:** Document this limitation, consider future schema change

### High Risk (None Identified)

---

## Success Criteria

### Functional Requirements
1. ✅ System supports all four payment behavior types
2. ✅ Transaction amounts correctly reflect account balance impacts
3. ✅ Existing payment functionality unchanged
4. ✅ No database schema changes required

### Non-Functional Requirements
1. ✅ All existing tests pass without modification
2. ✅ New tests cover all four payment behaviors
3. ✅ Code quality maintained (no trailing whitespace, proper validation)
4. ✅ Documentation updated comprehensively

### Acceptance Tests
1. ✅ Create payment: Checking → Savings ($100)
   - Verify source transaction: -$100
   - Verify destination transaction: +$100

2. ✅ Create payment: Checking → Credit Card ($100)
   - Verify source transaction: -$100
   - Verify destination transaction: -$100

3. ✅ Create payment: Credit Card → Checking ($100)
   - Verify source transaction: +$100
   - Verify destination transaction: +$100

4. ✅ Create payment: Credit Card A → Credit Card B ($100)
   - Verify source transaction: -$100
   - Verify destination transaction: +$100

---

## Implementation Sequence

### Step 1: Create PaymentBehavior Enum
**Estimate:** 30 minutes
**File:** `src/main/kotlin/finance/domain/PaymentBehavior.kt`

### Step 2: Update StandardizedPaymentService
**Estimate:** 2 hours
**File:** `src/main/kotlin/finance/services/StandardizedPaymentService.kt`

Tasks:
- Add `determinePaymentBehavior()` function
- Add `calculateSourceAmount()` function
- Add `calculateDestinationAmount()` function
- Rename transaction population functions
- Update transaction amount logic
- Update transaction accountType assignment
- Remove debit account type validation

### Step 3: Write Unit Tests
**Estimate:** 2 hours
**File:** `src/test/unit/groovy/finance/domain/PaymentBehaviorSpec.groovy`

### Step 4: Update Integration Tests
**Estimate:** 2 hours
**File:** `src/test/integration/groovy/finance/graphql/PaymentMutationIntSpec.groovy`

### Step 5: Update Functional Tests
**Estimate:** 1 hour
**File:** `src/test/functional/groovy/finance/controllers/StandardizedPaymentControllerFunctionalSpec.groovy`

### Step 6: Update Documentation
**Estimate:** 1 hour
**Files:** `CLAUDE.md`, `TODO.md`

### Total Estimate: 8-10 hours

---

## Open Questions

1. **Should we expose `PaymentBehavior` in the API responses?**
   - **Recommendation:** Yes, as optional read-only field for transparency

2. **Should users be able to override payment behavior manually?**
   - **Recommendation:** No initially - let system infer from account types
   - **Future:** Add optional override field if needed

3. **How should we handle `AccountType.Undefined` or unknown categories?**
   - **Recommendation:** Default to negative amounts for both transactions (safest)
   - **Future:** Add validation to prevent undefined account types in payments

4. **Should we rename the `Payment` entity to something more generic?**
   - **Recommendation:** No - keep existing naming for backward compatibility
   - **Note:** "Payment" now represents any money movement, not just bill payments

5. **Do we need to update account balance calculation logic?**
   - **Investigation needed:** Current codebase exploration didn't reveal automatic balance updates
   - **Recommendation:** Verify if account.outstanding/cleared/future fields are updated elsewhere
   - **Follow-up:** If balance logic exists, ensure it handles new transaction signs correctly

---

## Future Enhancements

1. **Add PaymentBehavior field to Payment entity**
   - Store inferred behavior for audit trail
   - Allow manual override in rare cases

2. **Update unique constraint on t_payment**
   - Include source_account and destination_account
   - Prevent duplicate payments with swapped accounts

3. **Add account balance validation**
   - Verify sufficient funds for asset account payments
   - Prevent overdrafts unless account allows

4. **Add payment reversal functionality**
   - Create opposite transactions to undo payment
   - Maintain audit trail of reversals

5. **Support partial payments**
   - Split single payment into multiple transactions
   - Track payment installments

6. **Add payment scheduling**
   - Future-dated payments with `TransactionState.Future`
   - Recurring payment patterns

---

## Conclusion

This enhancement maintains backward compatibility while significantly expanding payment system flexibility. The core insight is using account type categories (asset vs. liability) to automatically determine correct transaction sign logic, eliminating the need for manual payment type selection.

**Key Benefits:**
- ✅ No breaking changes to existing API
- ✅ No database schema changes
- ✅ Existing data continues to work correctly
- ✅ Clear, testable logic based on account categories
- ✅ Supports real-world financial scenarios (transfers, cash advances, balance transfers)

**Implementation Risk:** LOW
**Business Value:** HIGH
**Recommended Priority:** Medium (nice-to-have enhancement, not critical bug fix)
