# Multi-Tenancy Implementation Plan

## Context

The application is currently single-tenant - all data is globally accessible to any authenticated user. While `owner` TEXT columns exist on most tables, they are all NULL and unused. The `User` entity exists but is completely disconnected from financial data. Adding multiple users without tenant isolation would create severe IDOR vulnerabilities (any authenticated user could read/modify any other user's data).

**Goal:** Make every data entity scoped to its owner (the authenticated username), using the existing `owner` columns, and prevent cross-tenant data access at every layer.

**Tenant model:** Per-user (username from JWT = owner identifier). Categories and descriptions become per-tenant.

---

## Phase 1: Database Migration (V13)

**File:** `src/main/resources/db/migration/prod/V13__add-multi-tenant-owner.sql`

### 1a. Backfill existing data with default owner
```sql
UPDATE t_account SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_transaction SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_category SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_description SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_payment SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_transfer SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_validation_amount SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_receipt_image SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_pending_transaction SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_parameter SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
UPDATE t_transaction_categories SET owner = 'henninb@gmail.com' WHERE owner IS NULL;
```

### 1b. Make owner NOT NULL
```sql
ALTER TABLE t_account ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_transaction ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_category ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_description ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_payment ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_transfer ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_validation_amount ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_receipt_image ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_pending_transaction ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_parameter ALTER COLUMN owner SET NOT NULL;
ALTER TABLE t_transaction_categories ALTER COLUMN owner SET NOT NULL;
```

### 1c. Drop old unique constraints and add owner-scoped ones

**t_account:**
- Drop: `unique_account_name_owner_account_type` (was `UNIQUE(account_name_owner, account_type)`)
- Drop: `t_account_account_name_owner_key` (was `UNIQUE(account_name_owner)`)
- Drop: `unique_account_name_owner_account_id` (was `UNIQUE(account_id, account_name_owner, account_type)`)
- Add: `UNIQUE(owner, account_name_owner, account_type)` - same account name allowed for different owners
- Add: `UNIQUE(owner, account_id, account_name_owner, account_type)` - for FK from t_transaction

**t_category:**
- Drop: `t_category_category_name_key` (was `UNIQUE(category_name)`)
- Add: `UNIQUE(owner, category_name)` - same category name per owner

**t_description:**
- Drop: `t_description_description_name_key` (was `UNIQUE(description_name)`)
- Add: `UNIQUE(owner, description_name)` - same description name per owner

**t_transaction:**
- Drop: `transaction_constraint` (was `UNIQUE(account_name_owner, transaction_date, description, category, amount, notes)`)
- Add: `UNIQUE(owner, account_name_owner, transaction_date, description, category, amount, notes)`
- Update FK `fk_account_id_account_name_owner` to reference new account unique constraint
- Update FK `fk_category_name` - now references `(owner, category)` to `t_category(owner, category_name)` (compound FK)
- Update FK `fk_description_name` - now references `(owner, description)` to `t_description(owner, description_name)` (compound FK)

**t_payment:**
- Drop: `payment_constraint` (was `UNIQUE(account_name_owner, transaction_date, amount)`)
- Add: `UNIQUE(owner, account_name_owner, transaction_date, amount)`

**t_transfer:**
- Drop: `transfer_constraint` (was `UNIQUE(source_account, destination_account, transaction_date, amount)`)
- Add: `UNIQUE(owner, source_account, destination_account, transaction_date, amount)`

**t_parameter:**
- Drop: `t_parameter_parameter_name_key` (was `UNIQUE(parameter_name)`)
- Add: `UNIQUE(owner, parameter_name)`

**t_pending_transaction:**
- Drop: `unique_pending_transaction_fields` (was `UNIQUE(account_name_owner, transaction_date, description, amount)`)
- Add: `UNIQUE(owner, account_name_owner, transaction_date, description, amount)`

### 1d. Add indexes for owner-scoped queries
```sql
CREATE INDEX idx_account_owner ON t_account(owner);
CREATE INDEX idx_transaction_owner ON t_transaction(owner);
CREATE INDEX idx_category_owner ON t_category(owner);
CREATE INDEX idx_description_owner ON t_description(owner);
CREATE INDEX idx_payment_owner ON t_payment(owner);
CREATE INDEX idx_transfer_owner ON t_transfer(owner);
CREATE INDEX idx_validation_amount_owner ON t_validation_amount(owner);
CREATE INDEX idx_parameter_owner ON t_parameter(owner);
CREATE INDEX idx_pending_transaction_owner ON t_pending_transaction(owner);
```

### 1e. Update stored functions to include owner
- `rename_account_owner(p_old_name, p_new_name, p_owner)` - add owner filter
- `disable_account_owner(p_new_name, p_owner)` - add owner filter

### 1f. Foreign key considerations
The most complex change: `t_transaction.category` currently FKs to `t_category.category_name` (globally unique). With per-tenant categories, the FK must become a compound FK on `(owner, category)` -> `(owner, category_name)`. Same for `t_transaction.description` -> `t_description.description_name`.

Similarly, `t_transaction.(account_id, account_name_owner, account_type)` FKs to `t_account.(account_id, account_name_owner, account_type)`. The owner column is already present on both sides, so the FK can be extended to include owner.

---

## Phase 2: Entity Changes

### 2a. Add `owner` field to all entities
Model after the existing `FamilyMember.kt` pattern (line 43-46):

**Files to modify:**
- `src/main/kotlin/finance/domain/Account.kt` - Add `owner: String` field with `@Column(name = "owner", nullable = false)`
- `src/main/kotlin/finance/domain/Transaction.kt` - Same
- `src/main/kotlin/finance/domain/Category.kt` - Same
- `src/main/kotlin/finance/domain/Description.kt` - Same
- `src/main/kotlin/finance/domain/Payment.kt` - Same
- `src/main/kotlin/finance/domain/Transfer.kt` - Same
- `src/main/kotlin/finance/domain/ValidationAmount.kt` - Same
- `src/main/kotlin/finance/domain/ReceiptImage.kt` - Same
- `src/main/kotlin/finance/domain/PendingTransaction.kt` - Same
- `src/main/kotlin/finance/domain/Parameter.kt` - Same
- `src/main/kotlin/finance/domain/MedicalExpense.kt` - Add owner field (not in V01 schema but needed)

Each entity gets:
```kotlin
@Column(name = "owner", nullable = false)
@field:Size(min = 3, max = 100)
@field:Convert(converter = LowerCaseConverter::class)
var owner: String = ""
```

### 2b. Update unique constraints in JPA annotations
- `Account`: `@UniqueConstraint(columnNames = ["owner", "account_name_owner", "account_type"])`
- `Category`: `@UniqueConstraint(columnNames = ["owner", "category_name"])`
- etc.

---

## Phase 3: Tenant Context Utility

**New file:** `src/main/kotlin/finance/utils/TenantContext.kt`

```kotlin
object TenantContext {
    fun getCurrentOwner(): String {
        val auth = SecurityContextHolder.getContext().authentication
            ?: throw AccessDeniedException("Not authenticated")
        val username = auth.principal as? String
            ?: throw AccessDeniedException("Invalid principal")
        return username.lowercase()
    }
}
```

This extracts the username (already set as the principal in `JwtAuthenticationFilter.kt:112`) and uses it as the owner/tenant identifier.

---

## Phase 4: Repository Changes

Every repository query that returns data must include `owner` filtering. Every repository with native SQL queries must add `AND owner = :owner`.

**Files to modify:**
- `src/main/kotlin/finance/repositories/AccountRepository.kt`
  - `findByOwnerAndAccountNameOwner(owner, accountNameOwner)`
  - `findByOwnerAndActiveStatusOrderByAccountNameOwner(owner, activeStatus)`
  - `findByOwnerAndActiveStatusAndAccountType(owner, activeStatus, accountType)`
  - All native queries add `AND t_account.owner = :owner`
  - `updateTotalsForAllAccounts()` -> `updateTotalsForAccountsByOwner(owner)`

- `src/main/kotlin/finance/repositories/TransactionRepository.kt`
  - All finders add owner parameter
  - `findByOwnerAndGuid(owner, guid)`
  - `findByOwnerAndAccountNameOwnerAndActiveStatus(owner, accountNameOwner, activeStatus)`
  - Native queries add owner filter

- `src/main/kotlin/finance/repositories/PaymentRepository.kt`
- `src/main/kotlin/finance/repositories/TransferRepository.kt`
- `src/main/kotlin/finance/repositories/CategoryRepository.kt`
- `src/main/kotlin/finance/repositories/DescriptionRepository.kt`
- `src/main/kotlin/finance/repositories/ValidationAmountRepository.kt`
- `src/main/kotlin/finance/repositories/ReceiptImageRepository.kt`
- `src/main/kotlin/finance/repositories/ParameterRepository.kt`
- `src/main/kotlin/finance/repositories/PendingTransactionRepository.kt`
- `src/main/kotlin/finance/repositories/MedicalExpenseRepository.kt`
- `src/main/kotlin/finance/repositories/FamilyMemberRepository.kt` (already has owner pattern)
- `src/main/kotlin/finance/repositories/MedicalProviderRepository.kt` (shared/reference data - may stay global)

---

## Phase 5: Service Layer Changes

Every service method must:
1. Get the current owner via `TenantContext.getCurrentOwner()`
2. Pass owner to repository methods
3. Set `entity.owner = currentOwner` on creates
4. Validate `entity.owner == currentOwner` on updates/deletes

**Files to modify:**
- `src/main/kotlin/finance/services/AccountService.kt`
- `src/main/kotlin/finance/services/TransactionService.kt`
- `src/main/kotlin/finance/services/PaymentService.kt`
- `src/main/kotlin/finance/services/TransferService.kt`
- `src/main/kotlin/finance/services/CategoryService.kt`
- `src/main/kotlin/finance/services/DescriptionService.kt`
- `src/main/kotlin/finance/services/ValidationAmountService.kt`
- `src/main/kotlin/finance/services/ReceiptImageService.kt`
- `src/main/kotlin/finance/services/ParameterService.kt`
- `src/main/kotlin/finance/services/PendingTransactionService.kt`
- `src/main/kotlin/finance/services/MedicalExpenseService.kt`
- `src/main/kotlin/finance/services/FamilyMemberService.kt`

**IDOR prevention pattern (critical):**
```kotlin
fun findById(guid: String): ServiceResult<Transaction> {
    val owner = TenantContext.getCurrentOwner()
    val transaction = transactionRepository.findByOwnerAndGuid(owner, guid)
        ?: throw EntityNotFoundException("Transaction not found: $guid")
    return ServiceResult.Success(transaction)
}
```

Never look up by ID alone, then check owner after. Always include owner in the WHERE clause to prevent timing attacks and ensure the DB enforces isolation.

---

## Phase 6: Controller & GraphQL Changes

Controllers mostly delegate to services, so changes are minimal if services handle owner correctly. But:

1. **Remove any direct repository calls** from controllers (if any exist)
2. **GraphQL queries** need `@PreAuthorize` where missing
3. **Payment/Transfer creation** must validate that both source and destination accounts belong to the current owner

**Files to modify:**
- `src/main/kotlin/finance/controllers/graphql/GraphQLQueryController.kt` - add `@PreAuthorize` to queries
- `src/main/kotlin/finance/controllers/graphql/GraphQLMutationController.kt` - already has `@PreAuthorize`
- All REST controllers (minimal changes since services handle owner)

---

## Phase 7: Test Updates

### 7a. Update test helpers
- `BaseIntegrationSpec` needs to set SecurityContext with test owner before each test
- Repository tests need owner parameter in all calls
- Functional tests need to validate cross-tenant isolation

### 7b. Add IDOR-specific tests
New test class: `src/test/integration/groovy/finance/security/TenantIsolationIntSpec.groovy`
- Create data as user A, verify user B cannot access it
- Verify cross-tenant payment/transfer is rejected
- Verify queries never return other tenant's data

### 7c. Update existing tests
All existing tests that call repositories/services need to pass owner. The `testOwner` pattern already used in tests maps well to this.

---

## Verification

1. **Build:** `./gradlew clean build -x test`
2. **Unit tests:** `./gradlew test`
3. **Integration tests:** `SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest`
4. **Functional tests:** `SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest`
5. **Manual IDOR test:** Register two users, create data as user A, attempt to access as user B via REST and GraphQL
6. **Migration test:** Run Flyway migration against a copy of production data

---

## Implementation Order

Since the user asked to start with the database:

1. **V13 Flyway migration** (Phase 1) - backfill, NOT NULL, new constraints, indexes
2. **Entity `owner` fields** (Phase 2) - add to all domain classes
3. **TenantContext utility** (Phase 3) - extract owner from SecurityContext
4. **Repository owner filtering** (Phase 4) - add owner to all queries
5. **Service layer owner enforcement** (Phase 5) - set/validate owner
6. **Controller/GraphQL updates** (Phase 6) - auth on queries
7. **Tests** (Phase 7) - tenant isolation tests

---

## Files Summary

### New files:
- `src/main/resources/db/migration/prod/V13__add-multi-tenant-owner.sql`
- `src/main/kotlin/finance/utils/TenantContext.kt`
- `src/test/integration/groovy/finance/security/TenantIsolationIntSpec.groovy`

### Modified files (key):
- All 11 domain entities in `src/main/kotlin/finance/domain/`
- All 13 repositories in `src/main/kotlin/finance/repositories/`
- All 12 services in `src/main/kotlin/finance/services/`
- GraphQL controllers in `src/main/kotlin/finance/controllers/graphql/`
- Test base classes and existing test specs
