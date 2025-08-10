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

## Architecture Overview

### Technology Stack
- **Primary Language**: Kotlin 2.1.x
- **Test Language**: Groovy with Spock framework
- **Framework**: Spring Boot 3.5.x
- **Security**: Spring Security with JWT
- **Database**: PostgreSQL (prod/stage) or Oracle (prodora)
- **Build Tool**: Gradle 8.14.x
- **Messaging**: Apache Camel for file processing routes
- **Metrics**: Micrometer with InfluxDB
- **GraphQL**: Custom GraphQL implementation

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
- **Profiles**: `prod`, `stage`, `prodora`, `func`, `int`, `unit`, `perf`, `ora`

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
- `docker-compose-stage.yml` - Staging configuration

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
- 100% test coverage for critical business logic
- Integration tests for all database operations
- Performance tests for file processing operations
- Security tests for authentication and authorization

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