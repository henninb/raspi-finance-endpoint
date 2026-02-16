# Integration Test Fix Progress

## Starting State
- **184 tests total, 135 failed**
- Primary root cause identified in `TROUBLESHOOTING_INTEGRATION_TESTS.md`: Flyway migration conflicts with H2 + `ddl-auto: create`

## Fix Applied So Far

### 1. Disabled Flyway in `application-int.yml`
- **File**: `src/test/integration/resources/application-int.yml`
- **Change**: `spring.flyway.enabled: true` -> `spring.flyway.enabled: false`
- **Rationale**: `ddl-auto: create` already handles schema creation from JPA entities. Flyway migrations (designed for PostgreSQL) conflict with H2 in the test environment. Tests that previously passed all had `spring.flyway.enabled=false` via `@TestPropertySource`.
- **Result**: **417 tests total, 108 failed** (unlocked ~233 previously blocked tests, resolved ~27 failures)

## Current Failure Categories

### 22 Failing Test Classes

| Test Class | Base Class | Fails in Isolation? | Failure Type |
|---|---|---|---|
| `CategoryMutationIntSpec` | `BaseIntegrationSpec` | Class not found (wrong package?) | Build error |
| `CategoryQueryIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `DescriptionMutationIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `DescriptionQueryIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `ParameterMutationIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `ParameterQueryIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `PaymentMutationIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `TransferMutationIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `TransferQueryIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `ValidationAmountMutationIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `ValidationAmountQueryIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `GraphQLIdSerializationIntSpec` | `BaseIntegrationSpec` | Class not found | Build error |
| `DatabaseResilienceIntSpec` | Unknown | Class not found | Build error |
| `PaymentEdgeCasesIntSpec` | `BaseIntegrationSpec` | Not tested | Unknown |
| `PaymentTransactionVerificationIntSpec` | `BaseIntegrationSpec` | Not tested | Unknown |
| `TransferRepositoryIntSpec` | `BaseIntegrationSpec` | Not tested | Unknown |
| `SecurityIntSpec` | `BaseRestTemplateIntegrationSpec` | Yes (1 test) | Assertion error |
| `SecurityEndpointsIntSpec` | `BaseRestTemplateIntegrationSpec` | Yes (1 test) | Assertion error |
| `SecurityUserRepoServiceIntSpec` | `BaseRestTemplateIntegrationSpec` | Not tested | Unknown |
| `CsrfControllerIntSpec` | `BaseRestTemplateIntegrationSpec` | **PASSES** | Only fails in full suite |
| `HealthEndpointIntSpec` | `BaseRestTemplateIntegrationSpec` | Not tested | Unknown |
| `FamilyMemberServiceIntSpec` | `Specification` (direct) | Yes (3 tests) | Test logic bug (owner mismatch) |
| `TenantIsolationIntSpec` | `Specification` (direct) | Not tested | Unknown |

### Failure Pattern Analysis

#### Pattern 1: "Class not found" (Build error in 700ms)
- ~13 test classes fail immediately when run individually with `--tests "finance.controllers.graphql.XxxIntSpec"`
- Likely incorrect package path in the `--tests` filter, OR the test classes are in a different package
- Need to locate actual file paths for these test classes

#### Pattern 2: Test logic bugs (owner/tenant mismatch)
- `FamilyMemberServiceIntSpec`: Sets SecurityContext to `"owner_filter"` but creates test data with owners `"owner1"` and `"owner_test"`. The `findAllActive()` method uses `TenantContext.getCurrentOwner()` which returns `"owner_filter"`, so it finds nothing.
- `TenantIsolationIntSpec`: Similar tenant context issues

#### Pattern 3: Security assertion failures
- `SecurityIntSpec`: 1 of 17 tests fails (`test security filter chain integration`)
- `SecurityEndpointsIntSpec`: 1 of 10 tests fails (`test security filter chain with different endpoints`)
- These fail in isolation - genuine assertion errors

#### Pattern 4: Context contamination
- `CsrfControllerIntSpec`: Passes in isolation, fails in full suite
- Some `AccountRepositoryIntSpec` tests: Pass in isolation, fail in full suite
- Shared Spring context between test classes causes state leakage

## Test Infrastructure Summary

### Base Classes
1. **`BaseIntegrationSpec`** - `@SpringBootTest(RANDOM_PORT)`, `@Transactional`, `@EnableSharedInjection`, uses `TestDataManager`
   - Does NOT have `@TestPropertySource` (relies on `application-int.yml`)
   - Sets up security context with random `testOwner`
2. **`BaseRestTemplateIntegrationSpec`** - `@SpringBootTest(RANDOM_PORT)`, uses `RestTemplate` for HTTP calls
   - Has `@TestPropertySource` with: `server.port=0`, `management.server.port=8081`, `management.endpoints.web.exposure.include=*`, `spring.flyway.enabled=false`, `management.influx.metrics.export.enabled=false`, `management.endpoint.health.show-details=always`
3. **`Specification` (direct)** - Some test classes don't use a base class
   - `ServiceLayerIntSpec`: Has explicit `@TestPropertySource(properties = ["spring.flyway.enabled=false"])` - PASSES
   - `FamilyMemberServiceIntSpec`: No `@TestPropertySource` - has test logic bugs

### InfluxDB Shutdown Errors
- `InfluxMeterRegistry` logs `unable to create database 'mydb'` and `failed to send metrics to influx` during shutdown
- The YAML has `management.metrics.export.influx.enabled: false` but Spring Boot 4 uses the property path `management.influx.metrics.export.enabled`
- These are non-fatal log errors, not test failures

## Next Steps

1. **Locate actual file paths** for the "class not found" test classes (GraphQL mutation/query specs, DatabaseResilienceIntSpec)
2. **Fix FamilyMemberServiceIntSpec** test logic: align owner in test data with SecurityContext owner
3. **Investigate SecurityIntSpec and SecurityEndpointsIntSpec** assertion failures
4. **Fix InfluxDB property path** in `application-int.yml`: move from `management.metrics.export.influx.enabled` to `management.influx.metrics.export.enabled`
5. **Investigate context contamination** causing CsrfControllerIntSpec to fail in full suite
6. **Investigate TenantIsolationIntSpec** failures
7. **Run PaymentEdgeCasesIntSpec, PaymentTransactionVerificationIntSpec, TransferRepositoryIntSpec** in isolation to classify their failures
