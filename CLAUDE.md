# raspi-finance-endpoint Build Commands

## Build and Test Commands
- Build: `./gradlew clean build -x test`
- Build with all tests: `./gradlew clean build test integrationTest functionalTest`
- Run application: `./gradlew bootRun`
- Run unit tests: `./gradlew test`
- Run single test: `./gradlew test --tests "finance.domain.AccountSpec"`
- Run integration tests: `./gradlew integrationTest`
- Run functional tests: `./gradlew functionalTest`
- Run performance tests: `./gradlew performanceTest`

## Code Style Guidelines
- **Language**: Primary language is Kotlin with Groovy for tests
- **Naming**: Use lowerCamelCase for variables/functions, PascalCase for classes
- **Imports**: Group by package, alphabetically sorted
- **Types**: Strongly typed with explicit nullability
- **Error Handling**: Use exceptions for error conditions
- **Test Style**: Spock framework for tests using Groovy
- **Data Classes**: Use Kotlin data classes with validation annotations
- **Repository Pattern**: JPA repositories with Spring Data
- **Database**: PostgreSQL (prod/stage) or Oracle (prodora)