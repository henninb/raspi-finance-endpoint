# Code Review Findings

All findings have been resolved.

---

## ~~Finding 7~~ — `findAllActive()` endpoint returns empty list by design ✅ RESOLVED

**Location:** `src/main/kotlin/finance/controllers/TransactionController.kt:57-66`
**Priority:** Low

A GET endpoint documented as "returns empty list by design" occupies a URL, generates Swagger
documentation, and requires callers to know not to use it. It is dead weight.

**Fix:** Either implement it (forward to the paginated endpoint or return all transactions) or
remove it entirely. If the route must exist for compatibility, document the real behavior in
the `@Operation` annotation and return a meaningful response.

---

## ~~Finding 8~~ — `GraphQLMutationController` repeats `@PreAuthorize` on every method ✅ RESOLVED

**Location:** `src/main/kotlin/finance/controllers/graphql/GraphQLMutationController.kt:44`
**Priority:** High (security)

`GraphQLQueryController` correctly places `@PreAuthorize("hasAuthority('USER')")` at the class
level. `GraphQLMutationController` repeats it on every individual method. A new mutation method
added without the annotation would be silently unprotected.

**Fix:** Move the annotation to the class level:

```kotlin
@Controller
@Validated
@PreAuthorize("hasAuthority('USER')")
class GraphQLMutationController(...)
```

Remove the per-method `@PreAuthorize` repetitions.

---

## ~~Finding 9~~ — `createParameter` / `updateParameter` accept the JPA entity as a GraphQL `@Argument` ✅ RESOLVED

**Location:** `src/main/kotlin/finance/controllers/graphql/GraphQLMutationController.kt:216, 247`
**Priority:** Medium

The `Parameter` domain entity is used directly as a GraphQL input type. This bypasses the DTO
layer, exposes the persistence model to the GraphQL schema, and prevents adding input-only
validation without polluting the entity. It is inconsistent with how every other mutation is
handled (`PaymentInputDto`, `CategoryInputDto`, `AccountInputDto`, etc.).

**Fix:** Create `ParameterInputDto` in `finance.controllers.dto` with Jakarta validation
annotations. Map it to `Parameter` in the controller before passing to the service.

---

## ~~Finding 10~~ — `GraphQLQueryController` bypasses `ServiceResult` for three resolvers ✅ RESOLVED

**Location:** `src/main/kotlin/finance/controllers/graphql/GraphQLQueryController.kt:68-70, 126-127, 131-133`
**Priority:** Medium

Three query resolvers skip the `ServiceResult` pattern and call service methods directly:

```kotlin
// Direct Optional — errors bypass GraphQLExceptionHandler
return accountService.account(accountNameOwner).orElse(null)
return paymentService.findByPaymentId(paymentId).orElse(null)
return transferService.findAllTransfers()   // raw list, no error handling
```

All other resolvers in the same class properly unwrap `ServiceResult`. These three
inconsistencies mean errors surface as unhandled exceptions rather than going through
`GraphQLExceptionHandler`.

**Fix:** Update `accountService.account()`, `paymentService.findByPaymentId()`, and
`transferService.findAllTransfers()` to return `ServiceResult`. Unwrap them with exhaustive
`when` in the controller.

---

## ~~Finding 11~~ — `receiptImages()` / `receiptImage()` use `List<Any>` / `Any?` stubs ✅ RESOLVED

**Location:** `src/main/kotlin/finance/controllers/graphql/GraphQLQueryController.kt:206-217`
**Priority:** Medium

Type-erased return types signal unfinished work and will cause a schema mapping error at runtime
if the GraphQL schema declares typed `ReceiptImage` fields.

**Fix:** Either implement the resolvers returning `List<ReceiptImage>` / `ReceiptImage?` backed
by `ReceiptImageService`, or remove the methods from both the controller and the `.graphqls`
schema file if the feature is intentionally unsupported.

---

## ~~Finding 12~~ — `populateSourceTransaction` and `populateDestinationTransaction` are `public` ✅ RESOLVED

**Location:** `src/main/kotlin/finance/services/PaymentService.kt:366, 403`
**Priority:** Medium

These are internal transaction-building helpers. Exposing them in the public API allows external
callers to bypass the payment-behavior logic (`PaymentBehavior.inferBehavior`) and manually
construct partial transactions. They are only meaningful within the `save()` call chain.

**Fix:** Change both methods to `private`. The deprecated `populateDebitTransaction` and
`populateCreditTransaction` wrappers exist solely to delegate to these methods — remove them
entirely once the methods are private.

---

## ~~Finding 13~~ — Several `TransactionService` pipeline methods are `public` ✅ RESOLVED

**Location:** `src/main/kotlin/finance/services/TransactionService.kt:453-573`
**Priority:** Medium-High

The following methods are implementation details of the `save()` / `update()` pipeline but are
publicly accessible:

- `processAccount`
- `processCategory`
- `processDescription`
- `masterTransactionUpdater`
- `createDefaultCategory`
- `createDefaultDescription`

`masterTransactionUpdater` is especially risky — calling it directly bypasses the owner-scoping
check enforced by `update()`, which is a tenant-isolation bug waiting to happen.

**Fix:** Mark all as `private`. If any are genuinely needed across service boundaries, use
`internal` and document the reason. Add a cross-service integration test to cover any `internal`
usage.

---

## ~~Finding 14~~ — `insertFutureTransaction` makes two sequential service calls without a transaction boundary ✅ RESOLVED

**Location:** `src/main/kotlin/finance/controllers/TransactionController.kt:482-531`
**Priority:** Medium-High

The controller calls `createFutureTransactionStandardized()` and then `save()` as two separate
service operations. If `save()` fails after `createFutureTransactionStandardized()` succeeds,
the application is left in partial state — a future transaction object exists but was never
persisted — with no rollback.

**Fix:** Merge the two steps into a single `@Transactional` service method
`createAndSaveFutureTransaction()`. The controller calls the one method and maps its
`ServiceResult`.

---

## ~~Finding 15~~ — `logger` is not `private` in `GraphQLQueryController` ✅ RESOLVED

**Location:** `src/main/kotlin/finance/controllers/graphql/GraphQLQueryController.kt:49`
**Priority:** Low

```kotlin
// Current — exposes logger to subclasses and same-package code
val logger: Logger = LogManager.getLogger()

// Fix
private val logger: Logger = LogManager.getLogger()
```

Loggers should always be `private`. Exposing the logger allows external code to log under this
class's name, which makes log attribution unreliable.
