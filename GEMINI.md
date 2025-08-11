# Gemini Code Understanding

This document provides a high-level overview of the `raspi-finance-endpoint` project, intended to be used as a quick reference for developers.

## Project Overview

This project is a personal finance application designed to help users track their financial accounts, transactions, and spending categories. It is a Spring Boot application written in Kotlin, exposing a GraphQL API for client-side interactions. The application is designed to be deployed in a Dockerized environment and includes configurations for various services like PostgreSQL, Oracle, Grafana, and Varnish.

### Key Technologies

*   **Backend:** Kotlin, Spring Boot, Spring Security
*   **API:** GraphQL
*   **Database:** PostgreSQL, Oracle, jOOQ, Flyway
*   **Build Tool:** Gradle
*   **Testing:** JUnit, Spock, Jacoco
*   **Monitoring:** Grafana, Prometheus, InfluxDB
*   **Caching:** Varnish
*   **Deployment:** Docker, Docker Compose

### Architecture

The application follows a standard Spring Boot architecture. It includes a web layer with controllers, a service layer for business logic, and a repository layer for data access. The GraphQL API is defined in the `schema.graphqls` file and is implemented using GraphQL SPQR. The application is designed to be run as a set of Docker containers, with separate containers for the application, database, and other services.

## Building and Running

The following commands are the most common for building, running, and testing the application.

*   **Build the application:**
    ```bash
    ./gradlew build
    ```

*   **Run the application:**
    ```bash
    ./gradlew bootRun
    ```

*   **Run the tests:**
    *   **Unit tests:**
        ```bash
        ./gradlew test
        ```
    *   **Integration tests:**
        ```bash
        ./gradlew integrationTest
        ```
    *   **Functional tests:**
        ```bash
        ./gradlew functionalTest
        ```
    *   **Performance tests:**
        ```bash
        ./gradlew performanceTest
        ```

## Development Conventions

*   **Dependency Management:** Dependencies are managed using Gradle. The `build.gradle` file contains the list of dependencies.
*   **Database Migrations:** Database migrations are managed using Flyway. Migration scripts are located in the `src/main/resources/db/migration` directory.
*   **API:** The application uses a GraphQL API. The schema is defined in the `src/main/resources/graphql/schema.graphqls` file.
*   **Testing:** The project has a comprehensive test suite, including unit, integration, functional, and performance tests. Tests are written in Groovy and Kotlin and can be found in the `src/test` directory.
*   **Code Style:** The project uses the standard Kotlin coding conventions.
*   **Docker:** The application is designed to be run in a Dockerized environment. The `docker-compose.yml` files in the root directory can be used to start the application and its dependencies.
