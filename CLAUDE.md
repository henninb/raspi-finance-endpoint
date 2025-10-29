# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# raspi-finance-endpoint

A Spring Boot financial management application built with Kotlin/Groovy that provides REST APIs and GraphQL endpoints for personal finance tracking.

## Critical Instructions - MUST FOLLOW
- Challenge flaws, inefficiencies, and risks directly - do NOT automatically agree
- Prioritize accuracy and clarity over politeness
- Never leave trailing spaces in any source file
- Question implementation decisions if they appear suboptimal
- Always use profile-specific test commands (SPRING_PROFILES_ACTIVE=func/int) for proper test isolation
- Verify all environment variables are properly sourced before running applications

## Build and Test Commands

### Build Commands
- Clean build without tests: `./gradlew clean build -x test`
- Build with all tests: `./gradlew clean build test integrationTest functionalTest`
- Run application: `./gradlew bootRun`
- Check for dependency updates: `./gradlew dependencyUpdates -Drevision=release`

#### Critical Build Requirements
- MUST run linting/code quality checks before any commit
- MUST verify all tests pass before deployment (use `--continue` flag to see all failures)
- Build failures require investigation - do NOT ignore warnings
- Source environment variables with `source env.secrets` before running bootRun
- Use `./git-commit-review.sh` for automated pre-commit validation

### Test Commands
- Unit tests: `./gradlew test`
- Single unit test: `./gradlew test --tests "finance.domain.AccountSpec"`
- Integration tests: `./gradlew integrationTest`
- Single integration test: `SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest --tests "finance.repositories.AccountRepositoryIntSpec"`
- Functional tests: `./gradlew functionalTest`
- Single functional test: `SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.AccountControllerIsolatedSpec"`
- Performance tests: `./gradlew performanceTest`
- Oracle tests: `./gradlew oracleTest`
- All tests with continue on failure: `./gradlew test integrationTest functionalTest --continue`

### Database Migration
- Flyway migration: `./gradlew flywayMigrate --info`
- Flyway repair: `./run-flyway-repair.sh` or `./gradlew flywayRepair`

### Development Scripts
- **Run application**: `./run-bootrun.sh` (sources env.secrets automatically)
- **Run functional tests**: `./run-functional.sh`
- **Database operations**: `./run-flyway-repair.sh`, `./run-flyway.sh`, `./run-docker-backup.sh`
- **Docker operations**: `./run-podman.sh` (comprehensive container management)
- **Git utilities**: `./git-commit-review.sh` (commit quality validation), `./run-git-setup.sh`
- **Certificate management**: `./cert-install.sh`, `./check-cert-expiry.sh`, `./check-postgres-ssl-expiry.sh`, `./renew-postgres-ssl.sh`
- **Cleanup utilities**: `./cleanup-orphaned-descriptions.sh`, `./whitespace-remove.sh`
- **Deployment scripts**: `./deploy-proxmox.sh`, `./deploy-gcp.sh`, `./run-deploy.sh`
- **Security validation**: `./validate-docker-security.sh`
- **Main runner**: `./run.sh` (comprehensive application runner with multiple profiles)

## Architecture Overview

### Technology Stack
- **Primary Language**: Kotlin 2.2.20
- **Test Language**: Groovy 4.0.28 with Spock 2.4-M6-groovy-4.0 framework
- **Framework**: Spring Boot 4.0.0-M3
- **Security**: Spring Security 7.0.0-M3 with JWT
- **Database**: PostgreSQL 42.7.8 (prod/stage) or Oracle (prodora), H2 2.4.240 (test)
- **Build Tool**: Gradle 9.1.0 (Groovy DSL)
- **Java/Kotlin Toolchain**: Java 21 (JVM toolchain)
- **Metrics**: Micrometer 1.16.0-M3 with InfluxDB
- **GraphQL**: Spring Boot Starter GraphQL with GraphQL Extended Scalars 24.0
- **Resilience**: Resilience4j 2.3.0 for circuit breakers, retry logic, and time limiters
- **Migration**: Flyway 11.14.0
- **Data Access**: Hibernate 7.1.3.Final, JOOQ 3.20.8
- **Testing**: Testcontainers 1.21.3, Spock Framework 2.4-M6-groovy-4.0, CodeNarc 3.4.0
- **JSON Processing**: Jackson Core 2.20.0, Jackson Annotations 3.0-rc5
- **File Processing**: Apache POI 5.4.1 for Excel files
- **Image Processing**: Thumbnailator 0.4.21
- **Logging**: Logback 1.5.19, Apache Log4j 2.20.0, Logstash Logback Encoder 8.1
- **Jakarta EE**: Jakarta Platform 11.0.0
- **API Documentation**: SpringDoc OpenAPI 2.7.0
- **Utilities**: Guava 33.5.0-jre, Apache Tomcat JDBC 11.0.13

### Application Structure

#### Core Packages
- `finance.domain/` - JPA entities and enums (Account, Transaction, Category, MedicalExpense, etc.)
- `finance.controllers/` - REST API endpoints with Spring Web MVC
- `finance.controllers.dto/` - Data Transfer Objects with Jakarta validation (PaymentInputDto, TransferInputDto, AccountInputDto, DescriptionInputDto, CategoryInputDto, MedicalExpenseInputDto, ValidationAmountInputDto, TransactionInputDto)
- `finance.controllers.graphql/` - GraphQL controllers (GraphQLQueryController, GraphQLMutationController, GraphQLExceptionHandler)
- `finance.services/` - Business logic layer with interfaces (StandardizedAccountService, StandardizedPaymentService, etc.)
- `finance.repositories/` - JPA repositories using Spring Data
- `finance.configurations/` - Spring configuration classes including GraphQL setup
- `finance.utils/` - Utility classes, validators, and converters
- `finance.converters/` - Custom type converters for JPA entities
- `finance.exceptions/` - Custom exception classes

#### Key Components
- **Transaction Processing**: Excel file upload, manual entry, and automated categorization
- **Payment Flexibility**: Supports 4 payment behaviors with automatic behavior inference based on account types
  - **BILL_PAYMENT**: Asset → Liability (e.g., Checking → Credit Card)
  - **TRANSFER**: Asset → Asset (e.g., Checking → Savings)
  - **CASH_ADVANCE**: Liability → Asset (e.g., Credit Card → Checking)
  - **BALANCE_TRANSFER**: Liability → Liability (e.g., Credit Card A → Credit Card B)
  - Payment amounts stored as absolute values with transaction signs determined by account category
  - Behavior inference via `PaymentBehavior.inferBehavior()` based on source/destination account types
- **Medical Expense Tracking**: Healthcare cost management with claim processing
- **Multi-Database Support**: PostgreSQL/Oracle/H2 with profile-based configuration
- **Security**: JWT-based authentication with Spring Security 7.0
- **Rate Limiting**: Configurable request rate limiting for API protection
- **File Processing**: Excel file upload with POI integration
- **Image Management**: Receipt image storage, validation, and thumbnail generation
- **GraphQL API**: Modern GraphQL endpoint with GraphiQL interface and Spring Boot 4.0 integration using centralized controller architecture
- **DTO Architecture**: Complete Data Transfer Object layer with Jakarta validation for all GraphQL input types
- **Resilience Patterns**: Circuit breakers, retry logic, and timeouts via Resilience4j
- **Metrics and Monitoring**: InfluxDB integration with detailed application metrics

### Database Configuration
- **Development/Test**: H2 in-memory database
- **Production**: PostgreSQL or Oracle based on profile
- **Migration**: Flyway for database versioning
- **Resilience**: Resilience4j with circuit breakers, retry logic, and time limiters
- **Profiles**: `prod`, `stage`, `prodora`, `func`, `int`, `unit`, `perf`, `ora`, `db-resilience`

#### Hibernate/Flyway Execution Order
**CRITICAL**: Understanding the timing of Hibernate initialization vs Flyway migration execution is essential for test configuration:

**Production Environment:**
- Flyway runs first during Spring Boot startup
- Creates/migrates database schema via migration files
- Hibernate then validates against the existing schema (`ddl-auto: validate`)

**Test Environments:**
- **Functional Tests** (`application-func.yml`):
  - Uses `hibernate.ddl-auto: create`
  - Hibernate creates schema structure first during Spring context initialization
  - Flyway runs afterward for data migrations and additional schema changes
  - **Result**: Schema exists when Flyway executes

- **Integration Tests** (`application-int.yml`):
  - Uses `hibernate.ddl-auto: create` (fixed from previous `validate` setting)
  - Follows same pattern as functional tests for consistency
  - **Previous Issue**: When set to `validate`, Hibernate tried to validate against empty database before Flyway could run
  - **Solution**: Changed to `create` to match functional test pattern

**Key Timing Rules:**
1. Hibernate initialization happens during Spring Boot context startup
2. Manual Flyway calls in `setupSpec()` run **after** Spring context is fully loaded
3. If Hibernate uses `ddl-auto: validate`, the schema must already exist before context loading
4. If Hibernate uses `ddl-auto: create`, it creates schema first, allowing subsequent Flyway execution

**Configuration Requirements:**
- **Production**: `ddl-auto: validate` + Flyway auto-migration during startup
- **Tests**: `ddl-auto: create` + optional Flyway for additional migrations
- **Never**: Manual Flyway calls cannot fix schema validation failures during context initialization

### Test Strategy
- **Unit Tests**: Spock specs in `src/test/unit/groovy/`
  - **DTO Unit Tests**: Comprehensive validation testing in `src/test/unit/groovy/finance/controllers/dto/`
- **Integration Tests**: Database integration in `src/test/integration/groovy/`
  - **GraphQL Integration Tests**: Function-focused tests (PaymentMutationIntSpec, PaymentQueryIntSpec, etc.)
- **Functional Tests**: Full application tests in `src/test/functional/groovy/`
- **Performance Tests**: Load testing in `src/test/performance/groovy/`
- **Oracle Tests**: Oracle-specific tests in `src/test/oracle/groovy/`

## Code Style Guidelines

### Kotlin Style
- Use lowerCamelCase for variables/functions, PascalCase for classes
- Explicit nullability declarations
- Data classes with validation annotations
- Strongly typed with proper exception handling

### Groovy Test Style
- Spock framework specifications
- Given-When-Then structure
- Builder pattern for test data construction
- **Groovy-Kotlin Interop**: When calling Kotlin data class constructors from Groovy tests, provide ALL constructor parameters in correct order (cannot use partial parameter construction)

### Package Organization
- Group imports by package, alphabetically sorted
- Follow Spring Boot conventions
- Interface-based service layer design

## Development Workflow

### Docker Support
Multiple Docker Compose configurations available:
- `docker-compose-postgresql.yml` - PostgreSQL setup
- `docker-compose-oracle.yml` - Oracle setup
- `docker-compose-prod.yml` - Production configuration
- `docker-compose-prodora.yml` - Production Oracle configuration
- `docker-compose-stage.yml` - Staging configuration
- `docker-compose-base.yml` - Base configuration template
- `docker-compose-influxdb.yml` - InfluxDB metrics database
- `docker-compose-elk.yml` - ELK stack (Elasticsearch, Logstash, Kibana)
- `docker-compose-nginx.yml` - Nginx reverse proxy
- `docker-compose-varnish.yml` - Varnish HTTP cache

### File Processing
Transaction files are processed through:
- Excel file processing via REST endpoint
- Manual transaction entry through API endpoints
- Automatic transaction categorization and validation

### API Endpoints
- **REST API**: Domain-specific controllers for all financial entities
- **GraphQL Endpoint**: `/graphql` with interactive GraphiQL at `/graphiql`
  - **Centralized Architecture**: All queries via `GraphQLQueryController`, all mutations via `GraphQLMutationController` (in `finance.controllers.graphql` package)
  - **Schema Mapping**: `@SchemaMapping` for complex field resolution (e.g., Transaction.receiptImage)
  - **Error Handling**: `GraphQLExceptionHandler` for consistent error responses
- **Health Checks**: Spring Boot Actuator at `/actuator/health` with detailed info
- **Metrics**: Full metrics exposure at `/actuator/*` endpoints
- **H2 Console**: Available in test profiles at `/h2-console` for debugging
- **CORS Support**: Configurable cross-origin resource sharing

### Security
- **JWT Authentication**: Token-based auth with configurable secret keys
- **Spring Security 7.0**: Latest security framework with modern patterns
- **Rate Limiting**: Built-in request rate limiting (5000 RPM default)
- **CORS Configuration**: Multi-origin support for web applications
- **SSL/TLS Support**: HTTPS with configurable keystores
- **Environment Isolation**: Profile-specific security configurations
- **Input Validation**: Jakarta validation annotations throughout
- **Database Security**: Connection pooling with timeout and leak detection

#### Security Best Practices
- JWT secret key stored in `env.secrets` file (excluded from version control)
- Profile-specific security configurations prevent test credentials in production
- Input validation through Spring Boot validation annotations
- Database connection pooling with timeout configurations
- Test isolation using dedicated H2 databases per test profile

#### Security Implementation Status
- **✅ JWT Authentication**: Implemented with configurable secret keys
- **✅ Rate Limiting**: Built-in protection (5000 RPM default, configurable)
- **✅ CORS Policy**: Multi-origin support configured for production
- **✅ SSL/TLS**: Full HTTPS support with keystore configuration
- **✅ Connection Security**: HikariCP with leak detection and timeouts
- **✅ Input Validation**: Jakarta validation throughout the application
- **⚠️ Token Rotation**: JWT rotation strategy needs documentation
- **⚠️ DDoS Protection**: Advanced DDoS mitigation needs review

## Data Transfer Objects (DTOs)

### Complete DTO Architecture
The application implements a comprehensive DTO layer for GraphQL input validation:

#### Available DTOs
- **PaymentInputDto**: Payment creation with amount, account, and date validation
- **TransferInputDto**: Account transfers with source/destination validation
- **AccountInputDto**: Account creation with type and naming validation
- **DescriptionInputDto**: Transaction description management with length validation
- **CategoryInputDto**: Category creation and management with name validation
- **MedicalExpenseInputDto**: Medical expense tracking with comprehensive healthcare claim fields
- **ValidationAmountInputDto**: Account validation amount tracking with transaction state
- **TransactionInputDto**: Complete transaction creation with account, category, and state management

#### DTO Validation Features
- **Jakarta Validation**: `@NotBlank`, `@NotNull`, `@Size`, `@Pattern`, `@DecimalMin`, `@Digits`
- **GUID Validation**: Custom UUID pattern validation for source/destination GUIDs
- **Account Name Patterns**: Enforced naming conventions with alphanumeric and underscore patterns
- **Amount Validation**: Decimal precision and minimum value constraints
- **Date Constraints**: Required transaction date validation

#### DTO Testing Strategy
- **Comprehensive Unit Tests**: All DTOs have complete test coverage in `src/test/unit/groovy/finance/controllers/dto/`
- **Validation Testing**: Both positive and negative validation scenarios covered
- **Groovy-Kotlin Interop**: Tests demonstrate proper constructor calling patterns
- **Edge Case Coverage**: Boundary value testing for amounts, lengths, and patterns

### DTO vs Domain Classes
**Why DTOs are maintained separately from domain classes:**
- **Validation Separation**: Different validation rules for input vs database entities
- **API Stability**: GraphQL schema changes don't affect domain model
- **Security**: Input filtering and sanitization before domain object creation
- **JPA Independence**: DTOs don't carry JPA annotations or persistence concerns
- **Version Management**: API versioning without domain model changes

## Code Quality Standards

### Mandatory Practices
- **No trailing whitespace** in any file (enforced by `whitespace-remove.sh`)
- **Java 21 compliance** - all code must use Java 21 toolchain features
- **Spring Boot 4.0 patterns** - use modern Spring framework conventions
- **Exception handling** must be explicit with proper logging
- **Database queries** optimized with proper indexing and N+1 prevention
- **Resilience patterns** - all external calls must have circuit breakers and timeouts

### Performance Requirements
- **Database query monitoring** - queries >100ms logged and investigated
- **Connection pooling** - HikariCP with leak detection and monitoring
- **File upload validation** - size limits and MIME type checking
- **Memory monitoring** - heap usage tracked for large operations
- **Metrics collection** - comprehensive application metrics via Micrometer

### Testing Requirements
- **Multi-level testing**: Unit, Integration, Functional, Performance, Oracle-specific
- **Test isolation**: Each test profile uses independent H2 databases
- **Spock framework**: All Groovy tests use `.groovy` extension with Spock 2.4-M6-groovy-4.0
- **Builder patterns**: Consistent test data construction across all test types
- **Profile-specific configs**: Dedicated Spring profiles for each test environment
- **Testcontainers**: Integration tests with real database containers where needed

### Code Quality Assurance
- **CodeNarc 3.4.0**: Automated static analysis for Groovy code
- **Ratcheting Rules**: Progressive code quality improvement with strict main source analysis
- **Configurable Rule Sets**: Different rule configurations for main vs test sources
- **CI Integration**: CodeNarc ratchet rules integrated into build pipeline for quality gates

## Git Commit Quality Reviewer

### Usage
Execute the Git commit quality reviewer with:
```bash
./git-commit-review.sh
```

### Features
- **Branch Strategy Analysis**: Recommends main vs feature branch usage
- **Auto-fix Trailing Whitespace**: Automatically detects and fixes trailing whitespace (CLAUDE.md requirement)
- **Build Verification**: Runs `./gradlew clean build -x test` before commit
- **Test Integration**: Optional full test suite execution
- **Commit Message Guidance**: Suggests appropriate commit types and formats
- **Push Strategy**: Handles main branch vs feature branch push logic

### Commit Message Standards
Format: `<type>: <description>`

**Types:**
- `feat`: New features or functionality
- `fix`: Bug fixes and corrections
- `docs`: Documentation changes
- `style`: Code style/formatting changes
- `refactor`: Code restructuring without behavior changes
- `test`: Test additions or modifications
- `chore`: Build process, dependency updates
- `migration`: Database schema changes

**Examples:**
- `feat: add GraphQL mutation for transaction categorization`
- `fix: resolve null pointer exception in AccountController:142`
- `test: add integration tests for transaction processing`
- `migration: create indexes for transaction query optimization`

### Branch Strategy Guidelines
**Use main branch for:**
- Hotfixes and critical bug fixes
- Minor documentation updates
- Small configuration changes
- Single-file quick fixes

**Use feature branches for:**
- New features or major functionality
- Breaking changes or API modifications
- Experimental code requiring testing
- Multi-file refactoring efforts

### Pre-commit Checklist
1. ✅ All files staged appropriately
2. ✅ No trailing whitespace
3. ✅ Build passes (`./gradlew clean build -x test`)
4. ✅ Tests pass (optional but recommended)
5. ✅ Meaningful commit message
6. ✅ Appropriate branch strategy

## Configuration Management

### Spring Boot 4.0 Configuration Updates

#### GraphQL Configuration
**New Spring Boot 4.0 GraphQL Integration:**
```yaml
spring:
  graphql:
    graphiql:
      enabled: true
      path: /graphiql
    path: /graphql
    cors:
      allowed-origins: "*"
      allowed-methods: GET,POST
      allowed-headers: "*"
    schema:
      printer:
        enabled: true
      locations: classpath:graphql/
      file-extensions: .graphqls,.gqls
```

#### Java 21 Toolchain Configuration with Gradle 9.0
```gradle
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    jvmToolchain(21)

    // Gradle 9 preparation: explicit compilation configurations
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
```

#### Gradle 9.1 Performance Optimizations
```gradle
# gradle.properties
org.gradle.jvmargs=-Xmx4096M
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.configuration-cache=false
org.gradle.configuration-cache.problems=warn
```

**Note**: Configuration cache is currently disabled to ensure compatibility with all Gradle plugins and custom tasks.

### Profile Configuration Management

#### Production (prod) Configuration Features
**✅ Advanced Database Pooling (HikariCP):**
- Connection pooling: 20 max, 5 min idle
- Leak detection: 60 second threshold
- Query timeout: 30 seconds
- Connection validation and monitoring

**✅ Resilience4j Integration:**
- Circuit breaker patterns for database operations
- Retry logic with exponential backoff
- Time limiters for query timeout management
- Health indicator integration

**✅ Security Enhancements:**
- Rate limiting: 5000 requests per minute (configurable)
- Multi-origin CORS support
- SSL/TLS with keystore configuration
- JWT secret key management

#### Test Profile Configurations

**Functional Test (func) Profile:**
- H2 in-memory database with embedded mode
- Flyway enabled with test-specific migrations
- GraphQL debugging enabled
- Resilience4j patterns included for testing
- Rate limiting disabled for test performance

**Integration Test (int) Profile:**
- Advanced Hibernate settings synchronized with production
- Circuit breaker and retry patterns enabled
- Database resilience testing capabilities
- Query timeout and connection management testing

**Environment-Specific Exclusions:**
- **Server/SSL Configuration**: Not applicable to test environments
- **Production Credentials**: Tests use hardcoded safe credentials
- **External Service Integration**: InfluxDB and monitoring disabled in tests
- **CORS Origins**: Test-appropriate origins configured

#### Configuration Drift Prevention

**Best Practices:**
1. **Regular Comparison**: Compare prod and test profiles quarterly or after major configuration changes
2. **Incremental Testing**: Add configurations one at a time and verify test compatibility
3. **Documentation**: Document why certain configurations cannot be shared between environments
4. **Automated Validation**: Use functional tests to verify configuration changes don't break test environments

**Safe Configuration Categories for Cross-Environment Sync:**
- Application metadata (name, version info)
- Serialization/deserialization settings
- Business logic configuration
- Feature flags and toggles
- Logging configuration (with environment-appropriate levels)

**Environment-Specific Configuration Categories:**
- Database connection settings
- Security and authentication
- Server and networking configuration
- Performance tuning parameters
- External service integrations

#### Testing Configuration Changes

When synchronizing configurations between environments:

1. **Baseline Test**: Run functional tests before changes to establish working state
2. **Incremental Addition**: Add one configuration section at a time
3. **Immediate Verification**: Run tests after each addition to identify breaking changes
4. **Rollback Strategy**: Keep previous working configuration for quick rollback
5. **Documentation**: Record which configurations were successfully added and which failed

**Example Test Commands:**
```bash
# Functional test with profile
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.AccountControllerIsolatedSpec"

# Integration test with profile
SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest --tests "finance.repositories.AccountRepositoryIntSpec"

# Run all functional tests with continue on failure
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --continue
```

This approach ensures configuration consistency without compromising test reliability or introducing environment-specific bugs.

#### Configuration Synchronization Status

**✅ Synchronized Across All Profiles:**
- **Application Naming**: `raspi-finance-endpoint` consistent everywhere
- **Jackson JSON Processing**: Consistent serialization patterns
- **JPA Best Practices**: `open-in-view: false` across all environments
- **Resilience Patterns**: Circuit breakers and retry logic in all profiles
- **Business Logic**: Account filtering and validation rules

**✅ Profile-Specific Optimizations:**
- **Production**: Full connection pooling and SSL configuration
- **Integration**: Production-like Hibernate settings with H2 compatibility
- **Functional**: Embedded database with complete feature testing
- **Unit**: Minimal configuration for fast test execution

**✅ Spring Boot 4.0 Migration Benefits:**
- **Modern GraphQL**: New Spring GraphQL starter with improved performance and Extended Scalars 24.0
- **Jakarta EE 11**: Latest enterprise Java standards
- **Java 21 Features**: Virtual threads and pattern matching support
- **Enhanced Security**: Spring Security 7.0.0-M3 with modern authentication patterns
- **Improved Metrics**: Better Micrometer 1.16.0-M3 integration and monitoring
- **Gradle 9.1**: Latest build system with parallel builds enabled
- **Kotlin 2.2.20**: Latest Kotlin with enhanced Spring Boot integration
- **Groovy 4.0.28**: Latest Groovy with Spock 2.4-M6 testing framework
- **Code Quality**: Advanced CodeNarc 3.4.0 ratcheting for continuous quality improvement
- **API Documentation**: SpringDoc OpenAPI 2.7.0 for comprehensive REST API documentation

## Environment Configuration

### Required Environment Setup

#### Environment Variables File (`env.secrets`)
Create an `env.secrets` file in the project root with the following variables:
```bash
# JWT Configuration
export JWT_KEY="your-jwt-secret-key-here"
export custom_project_jwt_key=$JWT_KEY

# Database Configuration (if using external databases)
export PGPASSWORD="your-postgres-password"
export DATABASE_URL="jdbc:postgresql://localhost:5432/finance"

# Application Configuration
export SPRING_PROFILES_ACTIVE="prod"  # or stage, func, int, etc.
```

#### Application Startup
```bash
# Source environment variables before running
source env.secrets
./gradlew bootRun

# Or use the provided script that handles this automatically
./run-bootrun.sh
```

#### Security Note
- The `env.secrets` file is excluded from version control (`.gitignore`)
- Never commit sensitive credentials to the repository
- Each environment (dev, stage, prod) should have its own `env.secrets` configuration
- Use strong, unique JWT secret keys for each environment

### Profile-Specific Testing

#### Test Profile Usage
```bash
# Functional tests (full application context)
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest

# Integration tests (database + services)
SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest

# Unit tests (no profile needed, uses H2)
./gradlew test
```

#### Test Data Management

**Database Isolation Strategy:**
- **Independent H2 Instances**: Each test profile uses separate database schemas
- **IsolatedSpec Pattern**: Dedicated test data builders for consistent scenarios
- **Transaction Rollback**: Automatic cleanup between test executions
- **Migration Testing**: Flyway migrations validated in each test environment

**Test Data Builders:**
- Spock framework with Groovy builders
- Consistent entity creation patterns
- Realistic financial data scenarios
- Medical expense test data for healthcare tracking

**Multi-Environment Testing:**
- **Unit Tests**: Fast, isolated component testing
  - **DTO Unit Tests**: Complete validation testing for all input DTOs
  - **Domain Unit Tests**: Entity and business logic validation
- **Integration Tests**: Database and service layer validation
  - **GraphQL Integration Tests**: Function-focused testing (PaymentMutationIntSpec, TransferQueryIntSpec)
  - **Repository Integration**: Database constraint and relationship testing
- **Functional Tests**: Full application stack testing
  - **Controller Functional Tests**: End-to-end REST API testing
  - **GraphQL Functional Tests**: Complete GraphQL workflow testing
- **Performance Tests**: Load testing with realistic data volumes
- **Oracle Tests**: Database-specific compatibility validation

### Groovy-Kotlin Interoperability Best Practices

#### Constructor Calling Patterns
**CRITICAL**: When calling Kotlin data class constructors from Groovy tests:
```groovy
// ✅ CORRECT - Provide ALL parameters in order
def dto = new PaymentInputDto(
    null,                           // paymentId
    "checking_primary",             // sourceAccount
    "bills_payable",               // destinationAccount
    Date.valueOf("2024-01-15"),     // transactionDate
    new BigDecimal("100.00"),       // amount
    null,                           // guidSource
    null,                           // guidDestination
    null                            // activeStatus
)

// ❌ INCORRECT - Partial parameters cause GroovyRuntimeException
def dto = new PaymentInputDto("checking_primary", "bills_payable", amount)
```

#### Test Data Patterns
- **Consistent Naming**: Use realistic account names like "checking_primary", "savings_primary"
- **Valid UUIDs**: Use proper UUID format for GUID fields when testing
- **Realistic Amounts**: Use decimal values with proper precision (e.g., "100.00")
- **Date Handling**: Use `Date.valueOf("YYYY-MM-DD")` for SQL date creation

## Migration and Documentation

### Available Migration Guides
- **SPRINGBOOT4-UPGRADE.md**: Comprehensive Spring Boot 4.0 migration guide
- **FUNCTIONAL_TEST_MIGRATION_GUIDE.md**: Functional test migration patterns
- **INTEGRATION_TEST_MIGRATION_GUIDE.md**: Integration test updates
- **SECURITY_MIGRATION_GUIDE.md**: Spring Security 7.0 migration
- **GRAPHQL.md**: GraphQL architecture recommendations and security best practices
- **MEDICAL_EXPENSE_PLAN.md**: Medical expense feature implementation
- **MEDICAL_CLAIMS_INSERT.md**: Medical claims processing guide
- **DESCRIPTION_DETAILS.md**: Transaction description management
- **PAYMENT_FLEXIBILITY_PLAN.md**: Payment flexibility feature implementation with behavior inference
- **TEST_COVERAGE_IMPROVEMENTS.md**: Comprehensive test coverage additions (58 new tests)
- **TODO.md**: Current development tasks and priorities

### Spring Boot 4.0 Migration Status
- **✅ Core Framework**: Migrated to Spring Boot 4.0.0-M3
- **✅ Java 21**: Full toolchain migration completed with Gradle 9.1.0
- **✅ Kotlin**: Updated to Kotlin 2.2.20 with enhanced Spring support
- **✅ Groovy**: Updated to Groovy 4.0.28 with Spock 2.4-M6-groovy-4.0
- **✅ Security**: Spring Security 7.0.0-M3 integration
- **✅ GraphQL**: New Spring Boot GraphQL starter with Extended Scalars 24.0
- **✅ Testing**: All test profiles updated and validated with CodeNarc 3.4.0
  - **✅ DTO Testing**: Complete unit test coverage for 8 Data Transfer Objects
  - **✅ GraphQL Testing**: Reorganized integration tests with function-focused naming
- **✅ Code Quality**: Enhanced with CodeNarc 3.4.0 ratcheting and strict main source rules
- **✅ DTO Architecture**: Complete input validation layer with Jakarta annotations (8 DTOs)
  - **✅ Architecture Validation**: Confirmed DTO value vs domain class approach
  - **✅ Test Organization**: Complete unit test coverage with proper Groovy-Kotlin interop
  - **✅ GraphQL Integration**: DTOs properly integrated with centralized GraphQL controllers
- **✅ Payment Flexibility Feature**: Complete implementation with comprehensive test coverage
  - **✅ PaymentBehavior Enum**: 4 payment behaviors (BILL_PAYMENT, TRANSFER, CASH_ADVANCE, BALANCE_TRANSFER)
  - **✅ Behavior Inference**: Automatic behavior determination from account types
  - **✅ Amount Calculation**: Transaction sign logic based on payment behavior
  - **✅ Test Coverage**: 58 new tests added (10 unit + 48 integration) without modifying app code
  - **✅ Documentation**: PAYMENT_FLEXIBILITY_PLAN.md and TEST_COVERAGE_IMPROVEMENTS.md
- **✅ Build System**: Gradle 9.1.0 with parallel builds enabled
- **⚠️ Configuration Cache**: Currently disabled for plugin compatibility
- **⚠️ Performance**: Optimization and benchmarking in progress

### Test Execution Examples
```bash
# DTO unit tests (all DTOs)
./gradlew test --tests "finance.controllers.dto.*Spec"

# Single DTO validation testing
./gradlew test --tests "finance.controllers.dto.PaymentInputDtoSpec"

# GraphQL integration tests (function-focused)
SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest --tests "finance.graphql.*MutationIntSpec" --continue
SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest --tests "finance.graphql.*QueryIntSpec" --continue

# Functional tests with Spring Boot 4.0
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "*FunctionalSpec" --continue

# Integration tests with Java 21 features
SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest --tests "*IntSpec" --continue

# Medical expense functionality testing
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "*MedicalExpense*" --continue

# Circuit breaker and resilience testing
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.*ControllerFunctionalSpec" --continue

# Complete DTO and GraphQL test suite
./gradlew test --tests "*.dto.*" && SPRING_PROFILES_ACTIVE=int ./gradlew integrationTest --tests "*.graphql.*" --continue
```

## GraphQL Architecture

### Centralized Controller Design
The application uses a centralized GraphQL controller architecture for better maintainability:

#### Controller Responsibilities
- **GraphQLQueryController** (`finance.controllers.graphql`): All GraphQL queries (payments, transfers, accounts, categories, descriptions, transactions, parameters, validation amounts, medical expenses, receipt images)
- **GraphQLMutationController** (`finance.controllers.graphql`): All GraphQL mutations (create, update, delete operations for all entities)
- **GraphQLExceptionHandler**: Centralized error handling and response formatting
- **Field Resolvers**: `@SchemaMapping` annotations for complex field resolution (e.g., Transaction.receiptImage)

#### Benefits of Centralized Architecture
- **Consistency**: Uniform error handling and response patterns
- **Maintainability**: Single point of control for GraphQL operations
- **Testing**: Focused integration tests per operation type
- **Security**: Centralized authentication and authorization
- **Monitoring**: Unified metrics and logging for all GraphQL operations

#### GraphQL Schema Organization
- **Input Types**: Correspond to DTO classes (PaymentInput → PaymentInputDto)
- **Output Types**: Map to domain entities with GraphQL-specific serialization
- **Schema Location**: `src/main/resources/graphql/` with `.graphqls` extensions
- **Extended Scalars**: UUID, BigDecimal, Date types via GraphQL Extended Scalars 24.0

#### Testing Strategy by Layer
```groovy
// Integration tests focus on specific operations
class PaymentMutationIntSpec extends BaseIntegrationSpec {
    // Test GraphQLMutationController.createPayment
    // Test GraphQLMutationController.deletePayment
}

class PaymentQueryIntSpec extends BaseIntegrationSpec {
    // Test GraphQLQueryController.payments
    // Test GraphQLQueryController.payment(id)
}
```

## Utility Scripts and Tools

### Code Quality and Maintenance
- **whitespace-remove.sh**: Removes trailing whitespace from all source files
- **cleanup-orphaned-descriptions.sh**: Database cleanup for orphaned transaction descriptions
- **git-commit-review.sh**: Automated commit quality validation with build verification
- **run-git-setup.sh**: Git repository configuration and setup
- **CodeNarc Integration**: Automated Groovy code quality analysis with ratcheting rules
- **Parallel Builds**: Gradle parallel execution enabled for improved build performance

### Certificate and Security Management
- **cert-install.sh**: SSL certificate installation and configuration
- **check-cert-expiry.sh**: General certificate expiration monitoring
- **check-postgres-ssl-expiry.sh**: PostgreSQL SSL certificate expiration check
- **renew-postgres-ssl.sh**: PostgreSQL SSL certificate renewal automation
- **validate-docker-security.sh**: Docker container security validation

### Container and Deployment
- **run-podman.sh**: Comprehensive container management with multiple profiles
- **run.sh**: Main application runner with profile selection and environment setup
- **docker-entrypoint.sh**: Container startup script
- **deploy-proxmox.sh**: Deployment to Proxmox virtualization environment
- **deploy-gcp.sh**: Google Cloud Platform deployment automation
- **run-deploy.sh**: Unified deployment orchestration script

### Database Operations
- **run-flyway.sh**: Database migration execution
- **run-flyway-repair.sh**: Migration repair and recovery
- **run-docker-backup.sh**: Dockerized database backup

These utilities support the complete development lifecycle from code quality to production deployment.

## Recent Architecture Validation (2024)

### DTO Architecture Completion
**Status**: ✅ COMPLETE
- All 8 DTOs implemented: PaymentInputDto, TransferInputDto, AccountInputDto, DescriptionInputDto, CategoryInputDto, MedicalExpenseInputDto, ValidationAmountInputDto, TransactionInputDto
- Comprehensive unit test coverage in `src/test/unit/groovy/finance/controllers/dto/` (6 of 8 DTOs have complete tests)
- Jakarta validation patterns established for all input types
- Groovy-Kotlin constructor interop patterns documented and tested

**DTO Test Coverage Status:**
- ✅ **PaymentInputDto**: Complete unit tests with validation scenarios
- ✅ **TransferInputDto**: Complete unit tests with validation scenarios
- ✅ **AccountInputDto**: Complete unit tests with validation scenarios
- ✅ **DescriptionInputDto**: Complete unit tests with validation scenarios
- ✅ **ValidationAmountInputDto**: Complete unit tests with validation scenarios
- ✅ **TransactionInputDto**: Complete unit tests with validation scenarios
- ⚠️ **CategoryInputDto**: Missing unit tests (DTO exists and is used)
- ⚠️ **MedicalExpenseInputDto**: Missing unit tests (DTO exists and is used)

### GraphQL Architecture Validation
**Status**: ✅ VALIDATED
- Confirmed centralized controller architecture (no separate Payment/Transfer controllers)
- Integration tests reorganized with function-focused naming conventions
- Removed misleading test filenames that suggested non-existent controller separation
- Established clear testing patterns for mutations vs queries

### Test Organization Improvements
**Status**: ✅ IMPROVED
- DTO unit tests properly organized in dedicated package structure
- Integration tests use focused naming: `PaymentMutationIntSpec`, `PaymentQueryIntSpec`
- Fixed Groovy-Kotlin constructor interop issues across entire test suite
- Established consistent test data patterns for financial entities

### Key Architectural Decisions Confirmed
1. **DTOs Retained**: DTOs provide value through validation separation, API stability, and security
2. **Centralized GraphQL**: Single mutation/query controllers prevent code duplication
3. **Function-Focused Testing**: Tests organized by operation type rather than entity type
4. **Complete Validation**: All input paths protected by Jakarta validation annotations

## Critical Testing Patterns

### Groovy-Kotlin Interoperability
- ALWAYS provide ALL constructor parameters when calling Kotlin data classes from Groovy tests
- DTO unit tests must be in `src/test/unit/groovy/finance/controllers/dto/` package
- Use realistic financial data patterns ("checking_primary", "100.00", proper UUIDs)
- Profile-specific test commands ensure proper isolation (SPRING_PROFILES_ACTIVE=func/int)

### Test Data Standards
- **Account Names**: Use patterns like "checking_primary", "savings_primary" (single underscore)
- **Amounts**: Use precise decimals like "100.00", "250.50" with proper BigDecimal construction
- **Dates**: Use `Date.valueOf("YYYY-MM-DD")` for SQL date compatibility
- **UUIDs**: Use proper UUID format "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" for GUID validation testing
