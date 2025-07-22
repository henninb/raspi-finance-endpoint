# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# raspi-finance-endpoint

A Spring Boot financial management application built with Kotlin/Groovy that provides REST APIs and GraphQL endpoints for personal finance tracking.

## Build and Test Commands

### Build Commands
- Clean build without tests: `./gradlew clean build -x test`
- Build with all tests: `./gradlew clean build test integrationTest functionalTest`
- Run application: `./gradlew bootRun`
- Check for dependency updates: `./gradlew dependencyUpdates -Drevision=release`

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