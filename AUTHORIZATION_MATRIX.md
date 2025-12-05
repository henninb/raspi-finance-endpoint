# Authorization Matrix

This document defines the authorization requirements for all API endpoints in the raspi-finance-endpoint application.

## Security Architecture

The application implements **defense-in-depth** authorization through two layers:

1. **URL Pattern Security** - Configured in `SecurityConfiguration.kt`
2. **Method-Level Security** - `@PreAuthorize` annotations on controllers

Both layers must be properly configured. The method-level annotations provide a safety net in case URL patterns are misconfigured.

## REST API Endpoints

### Public Endpoints (No Authentication Required)

| Endpoint Pattern | Required Authority | Controller | Notes |
|-----------------|-------------------|------------|-------|
| `/api/login` | Public | LoginController | User authentication endpoint |
| `/api/register` | Public | LoginController | User registration endpoint |

### Protected Endpoints (Require USER Authority)

All endpoints below require `hasAuthority('USER')` - users must be authenticated with a valid JWT token.

| Endpoint Pattern | Required Authority | Controller | Operations |
|-----------------|-------------------|------------|------------|
| `/api/logout` | USER (URL-based) | LoginController | Logout and token blacklisting |
| `/api/account/**` | USER | AccountController | Account CRUD operations |
| `/api/transaction/**` | USER | TransactionController | Transaction management |
| `/api/category/**` | USER | CategoryController | Category management |
| `/api/description/**` | USER | DescriptionController | Description management |
| `/api/parameter/**` | USER | ParameterController | Parameter management |
| `/api/payment/**` | USER | PaymentController | Payment operations |
| `/api/validation/amount/**` | USER | ValidationAmountController | Validation amount operations |
| `/api/medical-expenses/**` | USER | MedicalExpenseController | Medical expense tracking |
| `/api/receipt/image/**` | USER | ReceiptImageController | Receipt image management |

### GraphQL Endpoints

| Endpoint Pattern | Required Authority | Controller | Notes |
|-----------------|-------------------|------------|-------|
| `/graphql` | USER (URL-based) | GraphQLQueryController, GraphQLMutationController | All queries and mutations |
| `/graphiql` | USER (URL-based) | N/A | GraphQL IDE interface |

## Authorization Implementation

### Method-Level Authorization

Controllers use class-level `@PreAuthorize("hasAuthority('USER')")` annotations to enforce authentication:

```kotlin
@RestController
@RequestMapping("/api/account")
@PreAuthorize("hasAuthority('USER')")
class AccountController(
    private val accountService: AccountService,
) : StandardizedBaseController() {
    // All methods require USER authority
}
```

### URL Pattern Authorization

Configured in `SecurityConfiguration.kt`:

```kotlin
http.authorizeHttpRequests { authorize ->
    authorize
        .requestMatchers("/api/login", "/api/register").permitAll()
        .requestMatchers("/api/**").hasAuthority("USER")
        .requestMatchers("/graphql", "/graphiql").hasAuthority("USER")
        .anyRequest().authenticated()
}
```

### LoginController Special Case

`LoginController` does **NOT** have class-level `@PreAuthorize` because it contains both public (`/api/login`, `/api/register`) and protected (`/api/logout`) endpoints.

- **Public endpoints**: Rely on URL pattern `permitAll()`
- **Protected logout**: Relies on URL pattern `hasAuthority("USER")`

## Authority Definitions

| Authority | Description | Granted To |
|-----------|-------------|-----------|
| `USER` | Standard authenticated user | All registered users after successful login |
| `ROLE_USER` | Spring Security role equivalent | Automatically granted alongside `USER` |

## Future Enhancements

Consider implementing role-based access control (RBAC) for more granular permissions:

- **ROLE_ADMIN** - Administrative operations (bulk delete, system configuration, user management)
- **ROLE_READ_ONLY** - View-only access for reports and audits
- **ROLE_MANAGER** - Advanced features (category/description merging, data exports)

### Example Future RBAC Implementation

```kotlin
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ADMIN')")
class AdminController {
    @DeleteMapping("/accounts/bulk")
    @PreAuthorize("hasAuthority('ADMIN')")
    fun bulkDeleteAccounts(): ResponseEntity<*> {
        // Admin-only operation
    }
}
```

## Security Testing

### Manual Testing

**Test unauthorized access (should return 401/403):**
```bash
# Without JWT token
curl https://api.bhenning.com/api/account/select/active
# Expected: 401 Unauthorized or 403 Forbidden
```

**Test authorized access (should return 200):**
```bash
# With valid JWT token
curl -H "Authorization: Bearer <jwt-token>" https://api.bhenning.com/api/account/select/active
# Expected: 200 OK with account data
```

### Automated Testing

See `src/test/functional/groovy/finance/security/AuthorizationSecurityFunctionalSpec.groovy` for functional tests.

## Audit Trail

| Date | Change | Author |
|------|--------|--------|
| 2025-12-05 | Initial implementation of method-level authorization | Claude Code |
| 2025-12-05 | Added @PreAuthorize to 9 REST controllers | Claude Code |

## References

- [Spring Security @PreAuthorize Documentation](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Spring Security Authorization Architecture](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html)
- INJECTION_ATTACK_PREVENTION_PLAN.md - Security vulnerability assessment
