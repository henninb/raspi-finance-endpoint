# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

# raspi-finance-endpoint

A Spring Boot financial management application built with Kotlin/Groovy providing REST APIs and GraphQL endpoints for personal finance tracking.

## Critical Instructions
- Challenge flaws, inefficiencies, and risks directly
- Never leave trailing spaces in any source file
- Always use profile-specific test commands (SPRING_PROFILES_ACTIVE=func/int)
- Source environment variables before running applications

## Build and Test Commands

### Build
- Clean build: `./gradlew clean build -x test`
- Full build: `./gradlew clean build test integrationTest functionalTest`
- Run: `./run-bootrun.sh` (auto-sources env.secrets)
- Updates: `./gradlew dependencyUpdates -Drevision=release`

### Test
- Unit: `./gradlew test --tests "finance.domain.AccountSpec"`
- Integration: `SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest --tests "finance.repositories.AccountRepositoryIntSpec"`
- Functional: `SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.AccountControllerIsolatedSpec"`
- All: `./gradlew test integrationTest functionalTest --continue`

### Database
- Migrate: `./gradlew flywayMigrate --info`
- Repair: `./run-flyway-repair.sh`

### Key Scripts
- `./git-commit-review.sh` - Pre-commit validation
- `./run-podman.sh` - Container management
- `./whitespace-remove.sh` - Remove trailing spaces

## Architecture

### Technology Stack
- **Core**: Kotlin 2.2.20, Spring Boot 4.0.0-M3, Java 21
- **Testing**: Groovy 4.0.28, Spock 2.4-M6, CodeNarc 3.4.0
- **Security**: Spring Security 7.0.0-M3 with JWT
- **Database**: PostgreSQL/Oracle (prod), H2 (test), Flyway 11.14.0
- **Build**: Gradle 9.1.0
- **GraphQL**: Spring GraphQL with Extended Scalars 24.0
- **Resilience**: Resilience4j 2.3.0

### Package Structure
- `finance.domain/` - JPA entities
- `finance.controllers/` - REST API endpoints
- `finance.controllers.dto/` - DTOs with Jakarta validation
- `finance.controllers.graphql/` - GraphQL controllers (centralized)
- `finance.services/` - Business logic
- `finance.repositories/` - JPA repositories

### Key Features
- **Payment Flexibility**: 4 behaviors (BILL_PAYMENT, TRANSFER, CASH_ADVANCE, BALANCE_TRANSFER) with automatic inference
- **Transaction Processing**: Excel upload, manual entry, automated categorization
- **Medical Expense Tracking**: Healthcare cost management
- **GraphQL API**: Centralized controller architecture at `/graphql` and `/graphiql`
- **Security**: JWT authentication, rate limiting (5000 RPM)
- **Resilience**: Circuit breakers, retry logic, timeouts

### Database
- **Profiles**: `prod`, `stage`, `prodora`, `func`, `int`, `unit`, `perf`, `ora`
- **Hibernate/Flyway**: Production uses `ddl-auto: validate` + Flyway first, Tests use `ddl-auto: create` + optional Flyway

### Test Strategy
- **Unit**: `src/test/unit/groovy/` - DTO validation, domain logic
- **Integration**: `src/test/integration/groovy/` - Database, GraphQL
- **Functional**: `src/test/functional/groovy/` - Full application
- **Performance**: `src/test/performance/groovy/` - Load testing
- **Oracle**: `src/test/oracle/groovy/` - Database-specific

## Code Style

### Kotlin
- lowerCamelCase for variables/functions, PascalCase for classes
- Explicit nullability, strong typing

### Groovy Tests
- Spock framework with Given-When-Then
- **CRITICAL**: Provide ALL constructor parameters when calling Kotlin data classes from Groovy (no partial construction)

## Environment Setup

### Required env.secrets File
```bash
export JWT_KEY="your-jwt-secret-key"
export custom_project_jwt_key=$JWT_KEY
export PGPASSWORD="your-postgres-password"
export SPRING_PROFILES_ACTIVE="prod"
```

### Security
- JWT secret in `env.secrets` (gitignored)
- Rate limiting: 5000 RPM
- Jakarta validation throughout
- HikariCP connection pooling

## DTOs

### Available DTOs (in `finance.controllers.dto/`)
PaymentInputDto, TransferInputDto, AccountInputDto, DescriptionInputDto, CategoryInputDto, MedicalExpenseInputDto, ValidationAmountInputDto, TransactionInputDto

### Validation
- Jakarta annotations: `@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@DecimalMin`
- UUID pattern validation for GUIDs
- Unit tests in `src/test/unit/groovy/finance/controllers/dto/`

### Why Separate DTOs?
- Different validation rules vs domain entities
- API stability (GraphQL schema changes don't affect domain)
- Security (input filtering before domain object creation)

## Code Quality

### Mandatory
- No trailing whitespace (`whitespace-remove.sh`)
- Java 21 compliance
- Explicit exception handling
- Resilience patterns (circuit breakers, timeouts)
- CodeNarc 3.4.0 with ratcheting rules

### Performance
- Log queries >100ms
- HikariCP monitoring
- Micrometer metrics

## Git Commits

### Commit Messages
Format: `<type>: <description>`

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `migration`

### Pre-commit
Use `./git-commit-review.sh` for:
- Auto-fix trailing whitespace
- Build verification
- Commit message guidance

## Groovy-Kotlin Interop

### Constructor Pattern
**CRITICAL**: Provide ALL parameters when calling Kotlin data classes from Groovy:

```groovy
// ✅ CORRECT
def dto = new PaymentInputDto(
    null, "checking_primary", "bills_payable",
    Date.valueOf("2024-01-15"), new BigDecimal("100.00"),
    null, null, null
)

// ❌ INCORRECT - causes GroovyRuntimeException
def dto = new PaymentInputDto("checking_primary", "bills_payable", amount)
```

### Test Data Standards
- Account names: "checking_primary", "savings_primary"
- Amounts: `new BigDecimal("100.00")`
- Dates: `Date.valueOf("YYYY-MM-DD")`
- UUIDs: Proper format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"

## Documentation

### Key Guides
- SPRINGBOOT4-UPGRADE.md - Migration guide
- GRAPHQL.md - Architecture and security
- PAYMENT_FLEXIBILITY_PLAN.md - Payment behaviors
- TODO.md - Current tasks

## GraphQL

### Centralized Controllers
- **GraphQLQueryController**: All queries (in `finance.controllers.graphql`)
- **GraphQLMutationController**: All mutations
- **GraphQLExceptionHandler**: Error handling
- Schema location: `src/main/resources/graphql/*.graphqls`
- Input types → DTOs, Output types → Domain entities
