# Troubleshooting Integration Tests

This document outlines the troubleshooting steps taken to resolve failing integration tests in `ExternalIntegrationsIntSpec`.

## Initial Problem

All 21 integration tests in `finance.services.ExternalIntegrationsIntSpec` were failing with various errors, primarily indicating issues with Actuator endpoint accessibility and application context startup.

## Attempt 1: Expose all Actuator endpoints for tests

The initial hypothesis was that Actuator endpoints were not properly exposed in the test environment.

*   **Action:** Added `management.endpoints.web.exposure.include=*` to the `@TestPropertySource` annotation in `src/test/integration/groovy/finance/BaseRestTemplateIntegrationSpec.groovy`.
*   **Result:** Tests still failed, but the primary error shifted to `org.flywaydb.core.internal.exception.FlywayMigrateException` with a `JdbcSQLSyntaxErrorException`. This indicated an issue with Flyway database migrations in the H2 in-memory database used for testing.

## Attempt 2: Disable Flyway for tests

Given the Flyway migration errors, the next step was to disable Flyway during integration tests.

*   **Action:** Added `spring.flyway.enabled=false` to the `@TestPropertySource` annotation in `src/test/integration/groovy/finance/BaseRestTemplateIntegrationSpec.groovy`.
*   **Result:** The Flyway error was resolved, but tests still failed. New errors included `org.springframework.web.client.HttpClientErrorException$Forbidden` (403 Forbidden) when accessing Actuator endpoints and `io.micrometer.influx.InfluxMeterRegistry - unable to create database 'mydb'` errors.

## Attempt 3: Disable InfluxDB metrics export and enable detailed health checks

The InfluxDB connection error indicated an attempt to connect to a non-existent service in the test environment. The `403 Forbidden` errors suggested that security was still active on Actuator endpoints.

*   **Action:** Added `management.influx.metrics.export.enabled=false` and `management.endpoint.health.show-details=always` to the `@TestPropertySource` annotation in `src/test/integration/groovy/finance/BaseRestTemplateIntegrationSpec.groovy`.
*   **Result:** The InfluxDB error was resolved, but the `403 Forbidden` errors persisted for several Actuator endpoints.

## Attempt 4: Create a dedicated security configuration for the `int` profile

To address the `403 Forbidden` errors, an attempt was made to provide a specific security configuration for the `int` profile that would permit all requests.

*   **Action:** Created a new file `src/test/integration/groovy/finance/config/IntegrationTestSecurityConfig.groovy` with `@TestConfiguration`, `@Profile("int")`, and a `SecurityFilterChain` that permitted all requests (`auth.anyRequest().permitAll()`).
*   **Result:** Tests still failed, but with a new error: `org.springframework.security.web.UnreachableFilterChainException`. This indicated a conflict where multiple `SecurityFilterChain` beans were being defined.

## Attempt 5: Exclude main `WebSecurityConfig` from `int` profile and revert `IntegrationTestSecurityConfig`

To resolve the `UnreachableFilterChainException`, the strategy was to prevent the main `WebSecurityConfig` from loading when the `int` profile was active.

*   **Action:** Added `@Profile("!int")` to the `WebSecurityConfig` class in `src/main/kotlin/finance/configurations/WebSecurityConfig.kt`. Reverted `IntegrationTestSecurityConfig.groovy` to its previous state (before changing it to `@Configuration`).
*   **Result:** Compilation error: `Unresolved reference 'Profile'` in `WebSecurityConfig.kt`.

## Attempt 6: Add missing import for `@Profile` in `WebSecurityConfig.kt`

The compilation error from Attempt 5 needed to be resolved.

*   **Action:** Added `import org.springframework.context.annotation.Profile` to `src/main/kotlin/finance/configurations/WebSecurityConfig.kt`.
*   **Result:** Compilation error resolved, but tests still failed with `org.springframework.beans.factory.NoSuchBeanDefinitionException`. This indicated that excluding `WebSecurityConfig` also removed other necessary beans (like `PasswordEncoder`, `CorsConfigurationSource`, etc.) that were required for the application context to start.

## Attempt 7: Create a complete security configuration in `IntegrationTestConfig.groovy`

To avoid `NoSuchBeanDefinitionException`, the new approach was to create a comprehensive test security configuration that included all necessary beans, essentially duplicating and overriding parts of the main `WebSecurityConfig`.

*   **Action:**
    *   Reverted changes to `src/main/kotlin/finance/configurations/WebSecurityConfig.kt` and `src/test/integration/groovy/finance/BaseRestTemplateIntegrationSpec.groovy` to their original state (using `git checkout`).
    *   Created `src/test/integration/groovy/finance/config/IntegrationTestConfig.groovy`. This file included a `SecurityFilterChain` permitting all requests, plus `PasswordEncoder`, `CorsConfigurationSource`, and filter beans adapted from `WebSecurityConfig.kt`.
    *   Re-added the properties for Actuator exposure, Flyway and InfluxDB disablement, and detailed health checks to `BaseRestTemplateIntegrationSpec.groovy`.
*   **Result:** Tests still failed with `403 Forbidden` errors, indicating the new comprehensive `IntegrationTestConfig` was still not effectively disabling security.

## Attempt 8: Add `@EnableWebSecurity` to `IntegrationTestConfig.groovy`

It was suspected that the `IntegrationTestConfig` might not be fully engaging Spring Security's web capabilities without the explicit annotation.

*   **Action:** Added `@EnableWebSecurity` to the `IntegrationTestConfig` class in `src/test/integration/groovy/finance/config/IntegrationTestConfig.groovy`.
*   **Result:** Tests still failed with `403 Forbidden` errors. The custom security configuration was still not taking precedence or overriding the main application's security.

## Attempt 9: Remove `IntegrationTestConfig.groovy` and set a fixed management port

The continuous failures with security overrides suggested a more fundamental issue with how Spring Boot was loading and prioritizing security configurations in the test context. A simplification of the test environment was attempted.

*   **Action:**
    *   Removed `src/test/integration/groovy/finance/config/IntegrationTestConfig.groovy`.
    *   Reverted `src/test/integration/groovy/finance/BaseRestTemplateIntegrationSpec.groovy` to its original state (using `git checkout`).
    *   Modified `src/test/integration/groovy/finance/BaseRestTemplateIntegrationSpec.groovy` to set `management.server.port=8081` (fixed port), along with the previous properties to expose all management endpoints, disable Flyway, disable InfluxDB, and enable detailed health checks.
*   **Result:** This step was interrupted before tests could be run, as the user requested documentation.

## Next Steps

The next step would be to build the application and attempt to run it separately from the tests, then execute the tests against the running application to isolate whether the problem is with the test setup, the application's security configuration, or how Spring Boot integrates them in the test environment.
