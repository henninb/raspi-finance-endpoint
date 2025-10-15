# Legacy to Modern API Migration Plan

**Project**: raspi-finance-endpoint
**Frontend**: nextjs-website
**Analysis Date**: 2025-10-15
**Status**: DRAFT - Ready for Review

## Executive Summary

This plan identifies **unused legacy endpoints** that can be safely deleted and provides a phased migration strategy for converting remaining legacy endpoints to modern/standardized REST patterns. The analysis identified 159+ total backend endpoints with approximately **46 legacy endpoints**, of which **14 can be immediately deleted** without frontend changes.

### Key Findings

| Category | Count | Notes |
|----------|-------|-------|
| **Total Backend Endpoints** | 159+ | Across 15 controllers |
| **Legacy Endpoints** | 46 | Action-based URL patterns |
| **Standardized Endpoints** | 55 | RESTful patterns |
| **Business Logic Endpoints** | 58+ | Domain-specific operations |
| **Frontend API Calls** | 50+ REST + GraphQL | Active usage |
| **Unused Legacy Endpoints** | 14 | Safe to delete immediately |
| **Legacy Endpoints Requiring Migration** | 15 | Frontend update needed first |

---

## Phase 1: Immediate Deletion (No Frontend Impact)

These legacy endpoints are **NOT** called by the frontend and can be safely deleted immediately.

### 1.1 PaymentController - DELETE ALL LEGACY (4 endpoints)

The frontend exclusively uses modern Payment endpoints.

```kotlin
// ❌ DELETE - Unused by frontend
@GetMapping("/select")
fun selectAllPayments(): ResponseEntity<List<Payment>>

// ❌ DELETE - Unused by frontend
@PostMapping("/insert")
fun insertPayment(@Valid @RequestBody payment: Payment): ResponseEntity<Payment>

// ❌ DELETE - Unused by frontend
@PutMapping("/update/{paymentId}")
fun updatePayment(@PathVariable paymentId: Long, @Valid @RequestBody payment: Payment): ResponseEntity<Payment>

// ❌ DELETE - Unused by frontend
@DeleteMapping("/delete/{paymentId}")
fun deleteByPaymentId(@PathVariable paymentId: Long): ResponseEntity<Payment>
```

**Frontend Usage**: ✅ Uses `POST /api/payment`, `PUT /api/payment/{paymentId}`, `DELETE /api/payment/{paymentId}`, `GET /api/payment/active`

**Risk**: Low - Frontend already migrated
**Testing**: Run functional tests after deletion
**Estimated Time**: 30 minutes

---

### 1.2 TransferController - DELETE ALL LEGACY (3 endpoints)

The frontend exclusively uses modern Transfer endpoints.

```kotlin
// ❌ DELETE - Unused by frontend
@GetMapping("/select")
fun selectAllTransfers(): ResponseEntity<List<Transfer>>

// ❌ DELETE - Unused by frontend
@PostMapping("/insert")
fun insertTransfer(@Valid @RequestBody transfer: Transfer): ResponseEntity<Transfer>

// ❌ DELETE - Unused by frontend
@DeleteMapping("/delete/{transferId}")
fun deleteByTransferId(@PathVariable transferId: Long): ResponseEntity<Transfer>
```

**Frontend Usage**: ✅ Uses `POST /api/transfer`, `PUT /api/transfer/{transferId}`, `DELETE /api/transfer/{transferId}`, `GET /api/transfer/active`

**Risk**: Low - Frontend already migrated
**Testing**: Run functional tests after deletion
**Estimated Time**: 30 minutes

---

### 1.3 CategoryController - DELETE ALL LEGACY (5 endpoints)

The frontend exclusively uses modern Category endpoints.

```kotlin
// ❌ DELETE - Unused by frontend
@GetMapping("/select/active")
fun categories(): ResponseEntity<List<Category>>

// ❌ DELETE - Unused by frontend
@GetMapping("/select/{category_name}")
fun category(@PathVariable("category_name") categoryName: String): ResponseEntity<Category>

// ❌ DELETE - Unused by frontend
@PostMapping("/insert")
fun insertCategory(@Valid @RequestBody category: Category): ResponseEntity<Category>

// ❌ DELETE - Unused by frontend
@PutMapping("/update/{category_name}")
fun updateCategory(@PathVariable("category_name") categoryName: String, @Valid @RequestBody category: Category): ResponseEntity<Category>

// ❌ DELETE - Unused by frontend
@DeleteMapping("/delete/{categoryName}")
fun deleteCategory(@PathVariable categoryName: String): ResponseEntity<Category>
```

**Frontend Usage**: ✅ Uses `GET /api/category/active`, `POST /api/category`, `PUT /api/category/{categoryName}`, `DELETE /api/category/{categoryName}`

**Risk**: Low - Frontend already migrated
**Testing**: Run functional tests after deletion
**Estimated Time**: 30 minutes

---

### 1.4 MedicalExpenseController - DELETE LEGACY ENDPOINTS (2 endpoints)

The frontend uses modern Medical Expense endpoints exclusively.

```kotlin
// ❌ DELETE - Unused by frontend
@PostMapping("/legacy")
fun insertMedicalExpense(@Valid @RequestBody medicalExpense: MedicalExpense): ResponseEntity<MedicalExpense>

// ❌ DELETE - Unused by frontend (conflicts with modern endpoint)
@PostMapping("/insert")
fun insertMedicalExpenseWithInsertEndpoint(@Valid @RequestBody medicalExpense: MedicalExpense): ResponseEntity<MedicalExpense>

// ⚠️ REVIEW - May be used in testing, but frontend uses GET /{medicalExpenseId}
@GetMapping("/select/{medicalExpenseId}")
fun getMedicalExpenseById(@PathVariable medicalExpenseId: Long): ResponseEntity<MedicalExpense>

// ⚠️ REVIEW - May be used in testing, but frontend uses PUT /{medicalExpenseId}
@PutMapping("/update/{medicalExpenseId}")
fun updateMedicalExpense(@PathVariable medicalExpenseId: Long, @Valid @RequestBody medicalExpense: MedicalExpense): ResponseEntity<MedicalExpense>

// ⚠️ REVIEW - May be used in testing, but frontend uses DELETE /{medicalExpenseId}
@DeleteMapping("/delete/{medicalExpenseId}")
fun softDeleteMedicalExpense(@PathVariable medicalExpenseId: Long): ResponseEntity<MedicalExpense>
```

**Frontend Usage**: ✅ Uses `GET /api/medical-expenses/active`, `POST /api/medical-expenses`, `PUT /api/medical-expenses/{medicalExpenseId}`, `DELETE /api/medical-expenses/{medicalExpenseId}`

**Risk**: Low for `/legacy` and duplicate `/insert`
**Risk**: Medium for `/select/`, `/update/`, `/delete/` (verify no test dependencies)
**Testing**: Run functional tests, check for test usage patterns
**Estimated Time**: 1 hour (includes test verification)

---

### Phase 1 Summary

**Total Endpoints to Delete**: 14 legacy endpoints
**Total Time Estimate**: 2-3 hours
**Prerequisites**: None - frontend already migrated
**Success Criteria**: All functional tests pass after deletion

**Deletion Checklist**:
- [ ] Delete PaymentController legacy endpoints (4)
- [ ] Delete TransferController legacy endpoints (3)
- [ ] Delete CategoryController legacy endpoints (5)
- [ ] Delete MedicalExpenseController legacy endpoints (2)
- [ ] Run full test suite: `SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --continue`
- [ ] Verify GraphQL endpoints still work
- [ ] Update OpenAPI/Swagger documentation
- [ ] Commit changes: `git commit -m "refactor: remove unused legacy endpoints for Payment, Transfer, Category, MedicalExpense"`

---

## Phase 2: Frontend Migration Required (15 endpoints)

These legacy endpoints are **actively used** by the frontend and require frontend migration before backend deletion.

### 2.1 AccountController - MIGRATE FRONTEND FIRST (5 endpoints)

**Priority**: HIGH - Core functionality
**Frontend Files to Update**:
- `lib/hooks/useAccountFetch.ts`
- `lib/hooks/useAccountInsert.ts`
- `lib/hooks/useAccountUpdate.ts`
- `lib/hooks/useAccountDelete.ts`
- `app/accounts/page.tsx`
- `app/accounts/[accountNameOwner]/page.tsx`

#### Current Frontend Usage (Legacy)
```typescript
// ❌ LEGACY - Update to modern endpoint
GET /api/account/select/active          → GET /api/account/active
POST /api/account/insert                → POST /api/account
PUT /api/account/update/{accountNameOwner} → PUT /api/account/{accountNameOwner}
DELETE /api/account/delete/{accountNameOwner} → DELETE /api/account/{accountNameOwner}
```

#### Backend Endpoints to Delete After Frontend Migration
```kotlin
// ❌ DELETE after frontend migration
@GetMapping("/select/active")
fun accounts(): ResponseEntity<List<Account>>

// ❌ DELETE after frontend migration
@GetMapping("/select/{accountNameOwner}")
fun account(@PathVariable accountNameOwner: String): ResponseEntity<Account>

// ❌ DELETE after frontend migration
@PostMapping("/insert")
fun insertAccount(@Valid @RequestBody account: Account): ResponseEntity<Account>

// ❌ DELETE after frontend migration
@PutMapping("/update/{accountNameOwner}")
fun updateAccount(@PathVariable accountNameOwner: String, @Valid @RequestBody account: Account): ResponseEntity<Account>

// ❌ DELETE after frontend migration
@DeleteMapping("/delete/{accountNameOwner}")
fun deleteAccount(@PathVariable accountNameOwner: String): ResponseEntity<Account>
```

**Migration Steps**:
1. Update frontend to use modern Account endpoints
2. Test frontend functionality thoroughly
3. Deploy frontend changes
4. Monitor for errors (1 week)
5. Delete backend legacy endpoints
6. Run functional tests

**Estimated Time**: 4-6 hours (frontend + testing)
**Risk**: Medium - Core functionality, high usage

---

### 2.2 TransactionController - MIGRATE FRONTEND FIRST (4 endpoints)

**Priority**: HIGH - Core functionality
**Frontend Files to Update**:
- `lib/hooks/useTransactionFetch.ts`
- `lib/hooks/useTransactionInsert.ts`
- `lib/hooks/useTransactionUpdate.ts`
- `lib/hooks/useTransactionDelete.ts`
- `app/transactions/page.tsx`
- `app/transactions/[guid]/page.tsx`

#### Current Frontend Usage (Legacy)
```typescript
// ❌ LEGACY - Update to modern endpoint
POST /api/transaction/insert            → POST /api/transaction
PUT /api/transaction/update/{guid}      → PUT /api/transaction/{guid}
DELETE /api/transaction/delete/{guid}   → DELETE /api/transaction/{guid}
GET /api/transaction/select/{guid}      → GET /api/transaction/{guid}
```

#### Backend Endpoints to Delete After Frontend Migration
```kotlin
// ❌ DELETE after frontend migration
@GetMapping("/select/{guid}")
fun findTransaction(@PathVariable guid: String): ResponseEntity<Transaction>

// ❌ DELETE after frontend migration
@PostMapping("/insert")
fun insertTransaction(@Valid @RequestBody transaction: Transaction): ResponseEntity<Transaction>

// ❌ DELETE after frontend migration
@PutMapping("/update/{guid}")
fun updateTransaction(@PathVariable guid: String, @Valid @RequestBody transaction: Transaction): ResponseEntity<Transaction>

// ❌ DELETE after frontend migration
@DeleteMapping("/delete/{guid}")
fun deleteTransaction(@PathVariable guid: String): ResponseEntity<Transaction>
```

**Migration Steps**:
1. Update frontend to use modern Transaction endpoints
2. Test transaction CRUD operations thoroughly
3. Verify transaction state updates still work
4. Deploy frontend changes
5. Monitor for errors (1 week)
6. Delete backend legacy endpoints

**Estimated Time**: 4-6 hours (frontend + testing)
**Risk**: Medium-High - Critical transaction management functionality

---

### 2.3 ValidationAmountController - PARTIAL MIGRATION (1 endpoint)

**Priority**: MEDIUM
**Frontend Files to Update**:
- `lib/hooks/useValidationAmountFetch.ts`
- `app/validation/page.tsx`

#### Current Frontend Usage (Mixed)
```typescript
// ❌ LEGACY - Migrate this one
GET /api/validation/amount/select/{accountNameOwner}/cleared → GET /api/validation/amount/active (with filtering)

// ✅ MODERN - Already using these
POST /api/validation/amount
PUT /api/validation/amount/{validationId}
DELETE /api/validation/amount/{validationId}
```

#### Backend Endpoint to Delete After Frontend Migration
```kotlin
// ❌ DELETE after frontend migration
@GetMapping("/select/{accountNameOwner}/{transactionStateValue}")
fun selectValidationAmountByAccountId(
    @PathVariable accountNameOwner: String,
    @PathVariable transactionStateValue: String
): ResponseEntity<ValidationAmount>
```

**Migration Note**: The modern endpoint returns all validation amounts. Frontend should filter by account locally or add query parameters.

**Alternative Approach**: Add query parameter support to modern endpoint:
```kotlin
// ✅ ADD THIS to support filtering
@GetMapping("/active")
fun findAllActive(
    @RequestParam(required = false) accountNameOwner: String?,
    @RequestParam(required = false) transactionState: String?
): ResponseEntity<List<ValidationAmount>>
```

**Estimated Time**: 2-3 hours
**Risk**: Low - Limited usage

---

### 2.4 PendingTransactionController - MIGRATE FRONTEND FIRST (4 endpoints)

**Priority**: MEDIUM
**Frontend Files to Update**:
- `lib/hooks/usePendingTransactionFetch.ts`
- `lib/hooks/usePendingTransactionInsert.ts`
- `lib/hooks/usePendingTransactionDelete.ts`
- `app/pending/page.tsx`

#### Current Frontend Usage (Legacy)
```typescript
// ❌ LEGACY - Update to modern endpoint
GET /api/pending/transaction/all               → GET /api/pending/transaction/active
POST /api/pending/transaction/insert           → POST /api/pending/transaction
PUT /api/pending/transaction/update/{pendingTransactionId} → PUT /api/pending/transaction/{pendingTransactionId}
DELETE /api/pending/transaction/delete/{id}    → DELETE /api/pending/transaction/{id}
DELETE /api/pending/transaction/delete/all     → (Batch delete via individual calls)
```

#### Backend Endpoints to Delete After Frontend Migration
```kotlin
// ❌ DELETE after frontend migration
@GetMapping("/all")
fun getAllPendingTransactions(): ResponseEntity<List<PendingTransaction>>

// ❌ DELETE after frontend migration
@PostMapping("/insert")
fun insertPendingTransaction(@Valid @RequestBody pendingTransaction: PendingTransaction): ResponseEntity<PendingTransaction>

// ❌ DELETE after frontend migration (returns 204 NO_CONTENT)
@DeleteMapping("/delete/{id}")
fun deletePendingTransaction(@PathVariable id: Long): ResponseEntity<Void>

// ❌ DELETE after frontend migration (or keep as business logic endpoint)
@DeleteMapping("/delete/all")
fun deleteAllPendingTransactions(): ResponseEntity<Void>
```

**Note**: The `DELETE /delete/all` endpoint may be useful as a business logic operation. Consider keeping it as a non-CRUD operation.

**Estimated Time**: 3-4 hours
**Risk**: Low-Medium - Moderate usage

---

### 2.5 FamilyMemberController - MIGRATE FRONTEND FIRST (1 endpoint)

**Priority**: LOW
**Frontend Files to Update**:
- `lib/hooks/useFamilyMemberInsert.ts`
- `app/family-members/page.tsx`

#### Current Frontend Usage (Legacy)
```typescript
// ❌ LEGACY - Update to modern endpoint
POST /api/family-members/insert → POST /api/family-members
```

#### Backend Endpoint to Delete After Frontend Migration
```kotlin
// ❌ DELETE after frontend migration
@PostMapping("/insert")
fun insert(@Valid @RequestBody familyMember: FamilyMember): ResponseEntity<*>
```

**Estimated Time**: 1 hour
**Risk**: Low - Limited usage

---

### Phase 2 Summary

**Total Endpoints Requiring Frontend Migration**: 15 endpoints
**Total Frontend Files to Update**: ~20-25 files
**Total Time Estimate**: 14-20 hours (includes testing and monitoring)
**Success Criteria**:
- All frontend functionality works with modern endpoints
- Zero console errors related to API calls
- All functional tests pass
- 1 week of production monitoring shows no issues

---

## Phase 3: Consistency and Cleanup

After Phases 1 and 2 are complete, address these architectural inconsistencies.

### 3.1 FamilyMemberController - Remove `/std/` Prefix

**Issue**: Standardized endpoints use `/std/` prefix which is inconsistent.

```kotlin
// ❌ CURRENT - Inconsistent prefix
@GetMapping("/std/{familyMemberId}")
fun findById(@PathVariable familyMemberId: Long): ResponseEntity<FamilyMember>

// ✅ DESIRED - Consistent with other controllers
@GetMapping("/{familyMemberId}")
fun findById(@PathVariable familyMemberId: Long): ResponseEntity<FamilyMember>
```

**Action Items**:
1. Remove `/std/` prefix from modern endpoints
2. Update frontend to use clean URLs
3. Update tests

**Estimated Time**: 2 hours
**Risk**: Low - Easy to test

---

### 3.2 ReceiptImageController - Modernize to StandardRestController

**Issue**: ReceiptImageController doesn't implement `StandardRestController` interface.

```kotlin
// ❌ CURRENT - Legacy pattern
@PostMapping("/insert")
fun insertReceiptImage(@RequestBody receiptImage: ReceiptImage): ResponseEntity<Map<String, String>>

@GetMapping("/select/{receipt_image_id}")
fun selectReceiptImage(@PathVariable("receipt_image_id") receiptImageId: Long): ResponseEntity<Map<String, Any>>

// ✅ DESIRED - Standardized pattern
@PostMapping("")
fun save(@Valid @RequestBody receiptImage: ReceiptImage): ResponseEntity<ReceiptImage>

@GetMapping("/{receiptImageId}")
fun findById(@PathVariable receiptImageId: Long): ResponseEntity<ReceiptImage>

// Plus: findAllActive(), update(), deleteById()
```

**Action Items**:
1. Implement `StandardRestController<ReceiptImage, Long>` interface
2. Update service layer to return `ServiceResult<ReceiptImage>`
3. Create standardized endpoints
4. Update frontend if needed
5. Delete legacy endpoints
6. Update tests

**Estimated Time**: 4-6 hours
**Risk**: Low-Medium - May need to coordinate with frontend

---

### 3.3 Path Parameter Naming - Snake Case to Camel Case

**Issue**: Some endpoints still use `snake_case` path parameters.

```kotlin
// ❌ CURRENT - Inconsistent naming
@GetMapping("/select/{category_name}")
fun category(@PathVariable("category_name") categoryName: String)

@GetMapping("/select/{receipt_image_id}")
fun selectReceiptImage(@PathVariable("receipt_image_id") receiptImageId: Long)

// ✅ DESIRED - Consistent camelCase
@GetMapping("/{categoryName}")
fun findById(@PathVariable categoryName: String)

@GetMapping("/{receiptImageId}")
fun findById(@PathVariable receiptImageId: Long)
```

**Action Items**:
1. Review all path parameters for `snake_case` usage
2. Update to `camelCase` consistently
3. Update frontend if needed
4. Update tests

**Estimated Time**: 2-3 hours
**Risk**: Low - Mostly legacy endpoints

---

### 3.4 Response Type Consistency

**Issue**: Some controllers return `ResponseEntity<*>` instead of typed responses.

```kotlin
// ❌ CURRENT - Untyped response
@GetMapping("/active")
fun findAllActive(): ResponseEntity<*>

// ✅ DESIRED - Typed response
@GetMapping("/active")
fun findAllActive(): ResponseEntity<List<Category>>
```

**Affected Controllers**:
- CategoryController
- ParameterController
- FamilyMemberController

**Action Items**:
1. Update return types to proper generic types
2. Verify response serialization still works
3. Update tests if needed

**Estimated Time**: 2 hours
**Risk**: Low - Mostly cosmetic

---

### Phase 3 Summary

**Total Cleanup Tasks**: 4 major items
**Total Time Estimate**: 10-13 hours
**Success Criteria**: Consistent RESTful patterns across all controllers

---

## Phase 4: Testing and Documentation

### 4.1 Comprehensive Testing Strategy

#### Unit Tests
- Verify all service layer methods return `ServiceResult` correctly
- Test validation logic for DTOs
- Test error handling paths

#### Integration Tests
```bash
# Run integration tests for affected controllers
SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest --tests "finance.repositories.*" --continue
```

#### Functional Tests
```bash
# Run functional tests for all controllers
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.*Spec" --continue

# Run specific controller functional tests
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.AccountControllerIsolatedSpec" --rerun-tasks
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.TransactionControllerFunctionalSpec" --rerun-tasks
```

#### GraphQL Tests
```bash
# Verify GraphQL endpoints still work
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.GraphqlSpec" --rerun-tasks
```

#### Frontend Tests
```bash
# Run frontend integration tests (if available)
cd /home/henninb/projects/github.com/henninb/nextjs-website
npm run test
npm run test:e2e
```

---

### 4.2 Documentation Updates

#### OpenAPI/Swagger Documentation
1. Update endpoint descriptions to indicate "standardized" vs "legacy"
2. Mark deprecated endpoints with `@Deprecated` annotation
3. Add sunset dates for legacy endpoints
4. Generate updated Swagger JSON

#### Migration Guide
Create `FRONTEND_API_MIGRATION_GUIDE.md` with:
1. Before/after endpoint mapping table
2. Request/response format changes
3. Status code changes
4. Error handling differences
5. Example frontend code updates

#### API Changelog
Document all API changes with:
- Version numbers
- Breaking changes
- Deprecation notices
- Migration timelines

---

### Phase 4 Summary

**Testing Time Estimate**: 6-8 hours
**Documentation Time Estimate**: 4-6 hours
**Total Time Estimate**: 10-14 hours

---

## Complete Timeline and Risk Assessment

### Overall Project Timeline

| Phase | Tasks | Time Estimate | Risk Level | Dependencies |
|-------|-------|---------------|------------|--------------|
| **Phase 1** | Delete unused legacy endpoints | 2-3 hours | Low | None |
| **Phase 2** | Migrate frontend to modern endpoints | 14-20 hours | Medium | Phase 1 complete |
| **Phase 3** | Consistency and cleanup | 10-13 hours | Low | Phase 2 complete |
| **Phase 4** | Testing and documentation | 10-14 hours | Low | Phases 1-3 complete |
| **TOTAL** | | **36-50 hours** | | |

### Estimated Calendar Time
- **Aggressive**: 1-2 weeks (full-time focus)
- **Realistic**: 3-4 weeks (part-time, with monitoring periods)
- **Conservative**: 6-8 weeks (with extended monitoring and staged rollout)

---

## Risk Mitigation Strategies

### 1. Feature Flags
Consider implementing feature flags to toggle between legacy and modern endpoints during migration:

```kotlin
@Configuration
class FeatureFlags {
    @Value("\${features.use-legacy-endpoints:false}")
    var useLegacyEndpoints: Boolean = false
}
```

### 2. Canary Deployment
Deploy frontend changes to small percentage of users first:
1. Deploy to 10% of users, monitor for 24 hours
2. Increase to 50% if no issues
3. Full rollout after 72 hours

### 3. Rollback Plan
Maintain ability to quickly rollback:
1. Keep legacy endpoints for 2-4 weeks after frontend migration
2. Use Git tags for easy version rollback
3. Have database backups ready
4. Document rollback procedures

### 4. Monitoring and Alerting
Set up monitoring for:
- API error rates by endpoint
- Response time changes
- 404 errors (might indicate broken frontend calls)
- Failed transactions
- User-reported issues

---

## Implementation Checklist

### Pre-Migration
- [ ] Review this plan with team
- [ ] Set up monitoring and alerting
- [ ] Create feature flags if needed
- [ ] Back up production database
- [ ] Document current API usage patterns
- [ ] Create rollback procedures

### Phase 1: Immediate Deletion
- [ ] Delete PaymentController legacy endpoints
- [ ] Delete TransferController legacy endpoints
- [ ] Delete CategoryController legacy endpoints
- [ ] Delete MedicalExpenseController legacy endpoints
- [ ] Run full test suite
- [ ] Deploy to staging
- [ ] Test in staging
- [ ] Deploy to production
- [ ] Monitor for 48 hours

### Phase 2: Frontend Migration
**For Each Controller (Account, Transaction, ValidationAmount, PendingTransaction, FamilyMember):**
- [ ] Identify all frontend files using legacy endpoints
- [ ] Update hooks/services to use modern endpoints
- [ ] Update component imports
- [ ] Test locally
- [ ] Run frontend tests
- [ ] Deploy to staging
- [ ] Test in staging
- [ ] Deploy to production (canary)
- [ ] Monitor for 1 week
- [ ] Delete backend legacy endpoints
- [ ] Run functional tests
- [ ] Deploy backend to production

### Phase 3: Consistency and Cleanup
- [ ] Remove `/std/` prefix from FamilyMemberController
- [ ] Modernize ReceiptImageController
- [ ] Convert path parameters to camelCase
- [ ] Fix response type consistency
- [ ] Update tests
- [ ] Deploy to staging
- [ ] Deploy to production

### Phase 4: Testing and Documentation
- [ ] Complete comprehensive testing
- [ ] Update OpenAPI/Swagger docs
- [ ] Create migration guide
- [ ] Update API changelog
- [ ] Document lessons learned

### Post-Migration
- [ ] Remove feature flags
- [ ] Clean up monitoring alerts
- [ ] Archive legacy code
- [ ] Retrospective meeting
- [ ] Update CLAUDE.md

---

## Success Metrics

### Quantitative Metrics
- **Zero production incidents** related to API migration
- **Zero increase** in API error rates
- **No degradation** in API response times
- **100% test coverage** maintained or improved
- **14 legacy endpoints deleted** in Phase 1
- **15 legacy endpoints migrated** in Phase 2

### Qualitative Metrics
- Frontend code is cleaner and more consistent
- Backend code follows RESTful best practices
- API documentation is comprehensive and accurate
- Team confidence in making future API changes

---

## Appendix A: Complete Endpoint Mapping

### Account Endpoints
| Legacy Endpoint | Modern Endpoint | Frontend Status | Action |
|----------------|-----------------|-----------------|--------|
| `GET /select/active` | `GET /active` | Using legacy | Migrate |
| `GET /select/{accountNameOwner}` | `GET /{accountNameOwner}` | Using legacy | Migrate |
| `POST /insert` | `POST /` | Using legacy | Migrate |
| `PUT /update/{accountNameOwner}` | `PUT /{accountNameOwner}` | Using legacy | Migrate |
| `DELETE /delete/{accountNameOwner}` | `DELETE /{accountNameOwner}` | Using legacy | Migrate |

### Payment Endpoints
| Legacy Endpoint | Modern Endpoint | Frontend Status | Action |
|----------------|-----------------|-----------------|--------|
| `GET /select` | `GET /active` | Using modern | DELETE |
| `POST /insert` | `POST /` | Using modern | DELETE |
| `PUT /update/{paymentId}` | `PUT /{paymentId}` | Using modern | DELETE |
| `DELETE /delete/{paymentId}` | `DELETE /{paymentId}` | Using modern | DELETE |

### Transfer Endpoints
| Legacy Endpoint | Modern Endpoint | Frontend Status | Action |
|----------------|-----------------|-----------------|--------|
| `GET /select` | `GET /active` | Using modern | DELETE |
| `POST /insert` | `POST /` | Using modern | DELETE |
| `DELETE /delete/{transferId}` | `DELETE /{transferId}` | Using modern | DELETE |

### Category Endpoints
| Legacy Endpoint | Modern Endpoint | Frontend Status | Action |
|----------------|-----------------|-----------------|--------|
| `GET /select/active` | `GET /active` | Using modern | DELETE |
| `GET /select/{category_name}` | `GET /{categoryName}` | Using modern | DELETE |
| `POST /insert` | `POST /` | Using modern | DELETE |
| `PUT /update/{category_name}` | `PUT /{categoryName}` | Using modern | DELETE |
| `DELETE /delete/{categoryName}` | `DELETE /{categoryName}` | Using modern | DELETE |

### Transaction Endpoints
| Legacy Endpoint | Modern Endpoint | Frontend Status | Action |
|----------------|-----------------|-----------------|--------|
| `GET /select/{guid}` | `GET /{guid}` | Using legacy | Migrate |
| `POST /insert` | `POST /` | Using legacy | Migrate |
| `PUT /update/{guid}` | `PUT /{guid}` | Using legacy | Migrate |
| `DELETE /delete/{guid}` | `DELETE /{guid}` | Using legacy | Migrate |

### Medical Expense Endpoints
| Legacy Endpoint | Modern Endpoint | Frontend Status | Action |
|----------------|-----------------|-----------------|--------|
| `POST /legacy` | `POST /` | Using modern | DELETE |
| `POST /insert` | `POST /` | Using modern | DELETE |
| `GET /select/{medicalExpenseId}` | `GET /{medicalExpenseId}` | Using modern | DELETE |
| `PUT /update/{medicalExpenseId}` | `PUT /{medicalExpenseId}` | Using modern | DELETE |
| `DELETE /delete/{medicalExpenseId}` | `DELETE /{medicalExpenseId}` | Using modern | DELETE |

### Validation Amount Endpoints
| Legacy Endpoint | Modern Endpoint | Frontend Status | Action |
|----------------|-----------------|-----------------|--------|
| `GET /select/{accountNameOwner}/{transactionStateValue}` | `GET /active` + filtering | Using legacy | Migrate |

### Pending Transaction Endpoints
| Legacy Endpoint | Modern Endpoint | Frontend Status | Action |
|----------------|-----------------|-----------------|--------|
| `GET /all` | `GET /active` | Using legacy | Migrate |
| `POST /insert` | `POST /` | Using legacy | Migrate |
| `DELETE /delete/{id}` | `DELETE /{id}` | Using legacy | Migrate |
| `DELETE /delete/all` | *Keep as business logic* | Using legacy | Keep/Migrate |

### Family Member Endpoints
| Legacy Endpoint | Modern Endpoint | Frontend Status | Action |
|----------------|-----------------|-----------------|--------|
| `POST /insert` | `POST /` | Using legacy | Migrate |

---

## Appendix B: Quick Reference Commands

### Build and Test Commands
```bash
# Clean build
./gradlew clean build -x test

# Unit tests
./gradlew test

# Integration tests
SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest --continue

# Functional tests
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --continue

# All tests
./gradlew test integrationTest functionalTest --continue
```

### Specific Controller Tests
```bash
# Account controller
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.AccountControllerIsolatedSpec" --rerun-tasks

# Payment controller
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.PaymentControllerFunctionalSpec" --rerun-tasks

# Transaction controller
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.TransactionControllerFunctionalSpec" --rerun-tasks
```

### Git Workflow
```bash
# Create feature branch
git checkout -b refactor/remove-legacy-endpoints-phase1

# Run pre-commit checks
./git-commit-review.sh

# Commit with appropriate message
git commit -m "refactor: remove unused Payment legacy endpoints"

# Push and create PR
git push -u origin refactor/remove-legacy-endpoints-phase1
```

---

## Questions and Concerns

If you have questions about this migration plan, consider these discussion topics:

1. **Timeline**: Is 3-4 weeks realistic for your team's capacity?
2. **Monitoring**: What monitoring tools are currently in place?
3. **Rollback**: What's the rollback window for production deployments?
4. **Feature Flags**: Should we implement feature flags for safer rollout?
5. **Frontend Team Coordination**: Who will handle frontend updates?
6. **Database Migrations**: Are there any database schema changes needed?
7. **Third-party Integrations**: Are any external systems using these APIs?

---

## Approval and Sign-off

**Plan Version**: 1.0
**Created By**: Claude Code Analysis
**Review Status**: DRAFT
**Approved By**: _________________________
**Approval Date**: _________________________
**Start Date**: _________________________
**Target Completion**: _________________________

---

**Document Status**: This plan is ready for team review and approval. Please provide feedback before beginning Phase 1 implementation.