# Repository Guidelines

## Project Structure & Modules
- Source: Kotlin Spring Boot app under `src/main/kotlin/finance` with subpackages like `controllers`, `services`, `repositories`, `domain`, `configurations`, `utils`, `exceptions`, `routes`, and `processors`.
- Resources: `src/main/resources` (Flyway, config, templates, etc.).
- Tests: Spock/Groovy suites in `src/test/{unit,integration,functional,performance,oracle}` with matching `resources` folders.
- Ops & scripts: Gradle files at root, `Dockerfile`, `docker-compose-*.yml`, env files (`env.*`), and helper scripts (`run-*.sh`).

## Build, Test, and Development
- Build: `./gradlew clean build` — compiles Kotlin and runs unit tests, outputs JAR to `build/libs`.
- Run locally: `./gradlew bootRun -Dspring.profiles.active=dev` (or `./run-bootrun.sh`).
- Health check: `curl -k https://localhost:8443/actuator/health`.
- DB migrations: `./gradlew flywayMigrate` (Flyway reads profile/config from env).
- Test suites:
  - Unit: `./gradlew test`
  - Integration: `./gradlew integrationTest`
  - Functional: `./gradlew functionalTest`
  - Performance: `./gradlew performanceTest`
  - Oracle: `./gradlew oracleTest`
  Reports: `build/reports/test/<suite>/`.

## Coding Style & Naming
- Language: Kotlin (JVM 21) with Spring Boot.
- Style: Kotlin official; 4‑space indent; `camelCase` for vars/functions; `PascalCase` for classes.
- Packages: under `finance.*`.
- Conventions: Controllers `*Controller.kt`, services `*Service.kt`, repositories `*Repository.kt`. Keep methods small and focused; prefer constructor injection.

## Testing Guidelines
- Frameworks: Spock (Groovy) with Spring Boot test support. Name specs `*Spec.groovy` in the appropriate suite folder.
- Profiles: test tasks set `SPRING_PROFILES_ACTIVE` per suite; override with `-Dspring.profiles.active=<profile>` if necessary.
- Coverage: add tests for new endpoints, services, and repository queries; include negative cases and validation paths.

## Commit & Pull Request Guidelines
- Commits: imperative subject (≤72 chars), optional scope/type (e.g., `feat`, `fix`, `chore`, `docs`).
  Example: `feat(transactions): add GraphQL query for transfers`.
- PRs: clear description, motivation, linked issues, test evidence (commands run, sample cURL/GraphQL), and rollback notes. Ensure all Gradle tests pass.

## Security & Configuration
- Secrets: never commit secrets; use `env.secrets` locally. Prefer profile‑specific `env.*` and Spring profiles.
- API: if API‑key auth is enabled, send `X-Api-Key` header. Avoid logging sensitive values.
- SSL/dev: local HTTPS runs on `:8443`; development certs live in `ssl/` and should not be reused in production.

