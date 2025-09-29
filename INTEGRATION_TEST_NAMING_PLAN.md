# Integration Test Naming Normalization Plan

## Findings
- GraphQL: mixed generic names and clearer role-based names.
- Repositories: “Migrated”, “Simple”, and “.backup” suffixes in filenames.
- Security: ad-hoc suffixes (“Working”, “Simple”) and mixed “Integration/Int” terminology.
- Services: mixed “IntegrationSpec” vs “IntSpec”; a couple of overly-generic filenames.
- Root specs: a few files missing `IntSpec` despite living in the integration suite.

## Suggested Convention
- Suffix: use `IntSpec.groovy` for all integration tests.
- Scope-first names: describe subject + focus; avoid status labels like “Migrated/Working/Simple”.
- GraphQL: use `XxxQuerySpec.groovy` and `XxxMutationSpec.groovy` for resolvers/controllers; append `IntSpec` if they hit endpoints or require full Spring context.

## Rename Suggestions

### GraphQL
- `GraphQLIntegrationSpec.groovy` → `GraphQLEndpointIntSpec.groovy` (introspection/GraphiQL coverage).
- `GraphQLIdSerializationSpec.groovy` → If needs Spring context: `GraphQLIdSerializationIntSpec.groovy`; if pure DTO/mapper, move to unit tests.

### Root (integration suite root)
- `HealthEndpointSpec.groovy` → `HealthEndpointIntSpec.groovy`.
- `RandomPortSpec.groovy` → `RandomPortIntSpec.groovy`.

### Services
- `ServiceLayerIntegrationSpec.groovy` → `ServiceLayerIntSpec.groovy` (or split by specific service under test).
- `ExternalIntegrationsSpec.groovy` → `ExternalIntegrationsIntSpec.groovy` (or `ActuatorMetricsIntSpec.groovy`).

### Security
- `SecurityIntegrationSpec.groovy` → `SecurityIntSpec.groovy` (main integration suite).
- `SecurityIntegrationWorkingSpec.groovy` → `SecurityUserRepoServiceIntSpec.groovy` (or split into `SecurityUserRepositoryIntSpec` and `SecurityUserServiceIntSpec`).
- `SecurityIntegrationSimpleSpec.groovy` → `SecurityEndpointsIntSpec.groovy`.

### Repositories
- Prefer canonical names; remove status words:
  - `AccountRepositoryMigratedIntSpec.groovy` → `AccountRepositoryIntSpec.groovy` (then delete obsolete/backup peers).
  - `AccountRepositorySimpleMigratedIntSpec.groovy` → `AccountRepositorySimpleIntSpec.groovy`.
  - `TransactionRepositoryMigratedIntSpec.groovy` → `TransactionRepositoryIntSpec.groovy`.
  - `TransactionRepositorySimpleMigratedIntSpec.groovy` → `TransactionRepositorySimpleIntSpec.groovy`.
- Delete backups: remove any `*.backup` files under `src/test/integration/groovy/finance/repositories/`.

## Cleanup
- Remove redundant/legacy files after renames:
  - Delete `*.backup` in `src/test/integration/groovy/finance/repositories/`.
  - Where a “Migrated” spec supersedes an older peer of the same subject, rename to the canonical name and delete the obsolete one.

## Optional Next Steps
- Apply the renames and deletions.
- Run `./gradlew integrationTest` and verify green.
- Update any documentation or CI filters that reference old filenames.

## Functional Repositories Plan

### Rationale (Why Move JPA Specs)
- Separation of concerns: Functional tests should cover HTTP/controller flows; JPA repository behavior belongs in integration.
- Consistency: Integration already has `*RepositoryIntSpec` coverage; centralizing DB-layer tests reduces duplication and confusion.
- CI/test profiles: Integration tests use the correct Spring profile and DB setup; functional remains focused on API behavior.

### Current Inventory
- Functional JPA specs (`src/test/functional/groovy/finance/repositories`):
  - AccountJpaSpec, CategoryJpaSpec, DescriptionJpaSpec, FamilyMemberJpaSpec, MedicalExpenseJpaSpec,
    ParameterJpaSpec, PaymentJpaSpec, PendingTransactionJpaSpec, ReceiptImageJpaSpec,
    TransactionJpaSpec, TransferJpaSpec, UserJpaSpec, ValidationAmountJpaSpec
- Integration repository specs (`src/test/integration/groovy/finance/repositories`):
  - AccountRepositoryIntSpec, AccountRepositorySimpleIntSpec, CategoryRepositoryIntSpec,
    DescriptionRepositoryIntSpec, MedicalExpenseRepositoryIntSpec, ParameterRepositoryIntSpec,
    PaymentRepositoryIntSpec, PendingTransactionRepositoryIntSpec, TransactionRepositoryIntSpec,
    TransactionRepositorySimpleIntSpec, TransferRepositoryIntSpec, UserRepositoryIntSpec,
    ValidationAmountRepositoryIntSpec
- Not present in integration yet: FamilyMember, ReceiptImage.

### Decision Rules
- If an integration spec for the same subject exists: merge any unique scenarios from the functional `*JpaSpec` into the integration `*RepositoryIntSpec` and delete the functional file.
- If no integration equivalent exists: move the functional `*JpaSpec` into the integration suite and rename to `*RepositoryIntSpec`.
- Only keep a functional `*JpaSpec` if it is intentionally used within a broader controller flow (rare); otherwise, prefer move/merge.

### Per-File Triage Map
- Move/Merge into existing integration specs:
  - AccountJpaSpec → AccountRepositoryIntSpec
  - CategoryJpaSpec → CategoryRepositoryIntSpec
  - DescriptionJpaSpec → DescriptionRepositoryIntSpec
  - MedicalExpenseJpaSpec → MedicalExpenseRepositoryIntSpec
  - ParameterJpaSpec → ParameterRepositoryIntSpec
  - PaymentJpaSpec → PaymentRepositoryIntSpec
  - PendingTransactionJpaSpec → PendingTransactionRepositoryIntSpec
  - TransactionJpaSpec → TransactionRepositoryIntSpec or TransactionRepositorySimpleIntSpec
  - TransferJpaSpec → TransferRepositoryIntSpec
  - UserJpaSpec → UserRepositoryIntSpec
  - ValidationAmountJpaSpec → ValidationAmountRepositoryIntSpec
- Create new integration specs (no current equivalent):
  - FamilyMemberJpaSpec → FamilyMemberRepositoryIntSpec
  - ReceiptImageJpaSpec → ReceiptImageRepositoryIntSpec

### Implementation Steps
1. Diff and map: Compare each functional `*JpaSpec` with the corresponding integration `*RepositoryIntSpec` to identify unique tests.
2. Merge: Add unique tests into the integration spec (preserve intent/comments); align base class and annotations.
3. Move/Create: For subjects without integration coverage, move to `integration/.../repositories` and rename to `*RepositoryIntSpec`.
4. Sanity check: Ensure packages are `finance.repositories` and class names match filenames.
5. Run suites: `./gradlew integrationTest` and `./gradlew functionalTest` to verify both suites are green.

### Acceptance Criteria
- No `*JpaSpec.groovy` remain under functional repositories.
- All repository tests live under integration and end with `IntSpec`.
- No duplicated repository coverage across suites.
- Both `integrationTest` and `functionalTest` pass.

### Rollback Plan
- If failures or environment flakiness occur, restore the moved file from Git and rerun functional tests while resolving integration profile/data issues.

### Execution Order
- Start with Account and Transaction (common cases) to validate the approach, then proceed across remaining entities.

## Phase 3: Migration Results (Status)

### Summary
- All functional `repositories/*JpaSpec.groovy` files removed after merge/move.
- Integration repository specs extended with missing constraint and delete cases.
- New integration specs added where gaps existed.
- Both `integrationTest` and `functionalTest` passed post-migration.

### Changes by Entity
- Account:
  - Added to `AccountRepositoryIntSpec`: invalid moniker; invalid `accountNameOwner` pattern.
  - Removed `functional/.../repositories/AccountJpaSpec.groovy`.
- Transaction:
  - Added to `TransactionRepositorySimpleIntSpec`: duplicate GUID exception; category length violation; invalid GUID format; delete removes record.
  - Removed `functional/.../repositories/TransactionJpaSpec.groovy`.
- Category:
  - Added to `CategoryRepositoryIntSpec`: invalid uppercase name triggers `ConstraintViolationException`.
  - Removed `functional/.../repositories/CategoryJpaSpec.groovy`.
- Description:
  - Redundant (integration already had non-existent finder); removed `DescriptionJpaSpec.groovy`.
- MedicalExpense:
  - Integration already covered FK, precision, finders; removed `MedicalExpenseJpaSpec.groovy`.
- Parameter:
  - Integration already covered non-existent finder and CRUD; removed `ParameterJpaSpec.groovy`.
- Payment:
  - Integration covered happy-path and constraints; removed `PaymentJpaSpec.groovy`.
- PendingTransaction:
  - Added to `PendingTransactionRepositoryIntSpec`: delete record test.
  - Removed `PendingTransactionJpaSpec.groovy`.
- Transfer:
  - Integration comprehensive; removed `TransferJpaSpec.groovy`.
- User:
  - Integration comprehensive; removed `UserJpaSpec.groovy`.
- ValidationAmount:
  - Integration comprehensive; removed `ValidationAmountJpaSpec.groovy`.
- FamilyMember (new):
  - Created `FamilyMemberRepositoryIntSpec.groovy` with CRUD/finders and active/soft delete path.
  - Removed `FamilyMemberJpaSpec.groovy`.
- ReceiptImage (new):
  - Created `ReceiptImageRepositoryIntSpec.groovy` with basic CRUD using `SmartReceiptImageBuilder`.
  - Removed `ReceiptImageJpaSpec.groovy`.

### Sanity Checks
- Class names match filenames (`*RepositoryIntSpec`).
- Packages remain under `finance.repositories`.
- No lingering imports from functional base classes.
- GraphQL, security, and service integration specs unaffected.

### How to Re-run Locally
- `./gradlew clean integrationTest`
- `./gradlew functionalTest`

### Notes / Follow-ups
- If future repository tests are added, place them under `integration/.../repositories` and end with `IntSpec`.
- Functional suite should remain focused on HTTP/controller flows; avoid adding DB-only repository tests there.
