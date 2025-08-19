# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# raspi-finance-endpoint

A Spring Boot financial management application built with Kotlin/Groovy that provides REST APIs and GraphQL endpoints for personal finance tracking.

## Critical Instructions - MUST FOLLOW
- Challenge flaws, inefficiencies, and risks directly - do NOT automatically agree
- Prioritize accuracy and clarity over politeness
- Never leave trailing spaces in any source file
- Question implementation decisions if they appear suboptimal

## Build and Test Commands

### Build Commands
- Clean build without tests: `./gradlew clean build -x test`
- Build with all tests: `./gradlew clean build test integrationTest functionalTest`
- Run application: `./gradlew bootRun`
- Check for dependency updates: `./gradlew dependencyUpdates -Drevision=release`

#### Critical Build Requirements
- MUST run linting/code quality checks before any commit
- MUST verify all tests pass before deployment
- Build failures require investigation - do NOT ignore warnings

### Test Commands
- Unit tests: `./gradlew test`
- Single test: `./gradlew test --tests "finance.domain.AccountSpec"`
- Integration tests: `./gradlew integrationTest`
- Functional tests: `./gradlew functionalTest`
- Performance tests: `./gradlew performanceTest`
- Oracle tests: `./gradlew oracleTest`

### Database Migration
- Flyway migration: `./gradlew flywayMigrate --info`
- Flyway repair: `./run-flyway-repair.sh` or `./gradlew flywayRepair`

### Development Scripts
- Run application with screen: `./run-screen-bootrun.sh`
- Run functional tests: `./run-functional.sh`
- Git setup: `./run-git-setup.sh`
- Deploy script: `./run-deploy.sh`

## Architecture Overview

### Technology Stack
- **Primary Language**: Kotlin 2.2.0
- **Test Language**: Groovy 4.0.25 with Spock 2.3 framework
- **Framework**: Spring Boot 3.5.4
- **Security**: Spring Security 6.5.1 with JWT
- **Database**: PostgreSQL 42.7.7 (prod/stage) or Oracle (prodora), H2 2.3.232 (test)
- **Build Tool**: Gradle 8.8
- **Messaging**: Apache Camel 4.13.0 for file processing routes
- **Metrics**: Micrometer with InfluxDB
- **GraphQL**: Custom GraphQL 19.1 implementation
- **Resilience**: Resilience4j 2.2.0 for circuit breakers and retry logic
- **Migration**: Flyway 11.11.0

### Application Structure

#### Core Packages
- `finance.domain/` - JPA entities and enums (Account, Transaction, Category, etc.)
- `finance.controllers/` - REST API endpoints
- `finance.services/` - Business logic layer with interfaces
- `finance.repositories/` - JPA repositories using Spring Data
- `finance.configurations/` - Spring configuration classes
- `finance.routes/` - Apache Camel route builders for file processing
- `finance.processors/` - Camel processors for transaction handling
- `finance.resolvers/` - GraphQL data fetchers
- `finance.utils/` - Utility classes and validators

#### Key Components
- **Transaction Processing**: File-based transaction import via Camel routes
- **Multi-Database Support**: Configurable PostgreSQL/Oracle support
- **Security**: JWT-based authentication with role-based access
- **File Processing**: Excel file upload and JSON transaction processing
- **Image Management**: Receipt image storage and validation
- **GraphQL API**: Query and mutation support for financial data

### Database Configuration
- **Development/Test**: H2 in-memory database
- **Production**: PostgreSQL or Oracle based on profile
- **Migration**: Flyway for database versioning
- **Resilience**: Resilience4j with circuit breakers, retry logic, and time limiters
- **Profiles**: `prod`, `stage`, `prodora`, `func`, `int`, `unit`, `perf`, `ora`, `db-resilience`

### Test Strategy
- **Unit Tests**: Spock specs in `src/test/unit/groovy/`
- **Integration Tests**: Database integration in `src/test/integration/groovy/`
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
Transaction files are processed through Camel routes:
- JSON input files in `json_in/`, `int_json_in/`, `func_json_in/`
- Excel file processing via REST endpoint
- Automatic transaction categorization and validation

### API Endpoints
- REST API at base path with controllers for each domain
- GraphQL endpoint at `/graphql` with GraphiQL at `/graphiql`
- Health checks at `/actuator/health`
- Metrics integration for monitoring

### Security
- JWT token-based authentication
- CORS configuration for cross-origin requests
- Role-based access control
- Request/response logging filters

#### Security Risks to Address
- JWT token storage and rotation strategy unclear
- CORS policy may be too permissive - needs review
- Input validation requirements not specified
- No mention of rate limiting or DDoS protection
- Database connection security not documented

## Code Quality Standards

### Mandatory Practices
- No trailing whitespace in any file
- All public methods must have proper documentation
- Exception handling must be explicit and meaningful
- Database queries must be optimized and reviewed for N+1 problems
- All external API calls must have timeout and retry logic

### Performance Requirements
- All database queries > 100ms must be logged and investigated
- File uploads must have size limits and validation
- Memory usage must be monitored for large data operations

### Testing Requirements
- **Coverage Goals**: ~70% functional test coverage target, current ~40%
- **Integration tests** for all database operations with H2 in-memory database
- **Performance tests** for file processing operations using dedicated profile
- **Security tests** for authentication and authorization flows
- **Test Naming**: All Groovy test files use `Spec.groovy` suffix (Spock framework)
- **Builder Pattern**: Use dedicated builder classes for test data construction
- **Test Profiles**: Each test type has dedicated Spring profile configuration

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
- `test: add integration tests for Camel route processing`
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

### Profile Configuration Drift Management

Configuration drift between production and test profiles can lead to environment-specific bugs and inconsistencies. Regular comparison and synchronization of configuration settings ensures reliable deployment and testing.

#### Profile Alignment Status

**Production (prod) → Functional Test (func) Configuration Sync:**

**✅ Successfully Synchronized Configurations:**
- **Spring Application Name**: `raspi-finance-endpoint` - ensures consistent application naming across environments
- **Jackson Configuration**: Property naming strategy (`LOWER_CAMEL_CASE`), null handling (`non_null`), enum case handling, and timezone settings (`America/Chicago`) - ensures consistent JSON serialization
- **JPA Configuration**: `open-in-view: false` - prevents lazy loading issues and follows best practices
- **Excluded Accounts**: `test_brian` - maintains consistency with production data filtering
- **Flyway Validation**: `baseline-on-migrate`, `baseline-version: 0`, `validate-on-migrate: true` - ensures database migration consistency

**❌ Configurations Not Compatible with Test Environment:**
- **Advanced Hibernate Settings**: Query timeouts, batch processing, and connection provider settings - these conflict with H2 in-memory database behavior
- **Production Database Pooling**: HikariCP production settings - not applicable to H2 test database
- **Server Configuration**: SSL, ports, and production server settings - tests use random ports and HTTP
- **Security Configuration**: Production authentication settings - tests use auto-generated credentials
- **SQL Init Configuration**: Production-specific SQL initialization - conflicts with test data setup

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

**Example Test Command:**
```bash
SPRING_PROFILES_ACTIVE=func ./gradlew functionalTest --tests "finance.controllers.AccountControllerSpec"
```

This approach ensures configuration consistency without compromising test reliability or introducing environment-specific bugs.

**Production (prod) → Integration Test (int) Configuration Sync:**

**✅ Successfully Synchronized Configurations:**
- **SQL Init Mode**: Added `mode: never` setting for consistency with production SQL initialization behavior
- **Advanced JPA/Hibernate Configuration**: Successfully added query timeouts (`query.timeout: 30000`), connection provider settings (`connection.provider_disables_autocommit: true`), JDBC batch processing (`batch_size: 20`, `batch_versioned_data: true`, `time_zone: UTC`), and query plan cache settings (`query.plan_cache_max_size: 2048`, `query.plan_parameter_metadata_max_size: 128`)
- **Resilience4j Configuration**: Added complete circuit breaker, retry, and time limiter configurations for database operations - critical for integration testing of production-like resilience patterns

**❌ Configurations Not Compatible with Integration Test Environment:**
- **Server Configuration**: SSL, address, and port settings - integration tests use random ports and HTTP
- **Security Configuration**: Production authentication settings - integration tests use hardcoded test credentials
- **Production Database Pooling**: Full HikariCP production settings - not all applicable to H2 test database
- **Allowed Origins**: CORS configuration - not relevant for integration test environment
- **External Service Configuration**: InfluxDB and monitoring settings - kept disabled for integration tests

**Integration Test Results:**
- ✅ All integration tests pass (CamelSpec and others)
- ✅ Database operations work correctly with new Hibernate settings
- ✅ Circuit breaker and resilience patterns function properly
- ✅ Transaction processing through Camel routes operates as expected
- ✅ No performance degradation observed

**Key Benefits of Int Profile Sync:**
- **Resilience Testing**: Integration tests now include the same circuit breaker and retry logic as production
- **Performance Consistency**: Query timeouts and batch processing behavior matches production
- **Database Behavior**: Connection management and transaction handling aligns with production settings
- **Early Issue Detection**: Integration tests can catch resilience-related issues before deployment