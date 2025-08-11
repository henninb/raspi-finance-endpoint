## 1. Controller Test Coverage (Spock)
Role: Expert Spock tester  
Instructions:
- Achieve **100% test coverage** for all controllers without changing functionality.
- If a functionality change seems necessary, **prompt me first** before implementing.
- Follow consistent patterns and industry best practices for Spock tests.
- Ensure tests are clear, maintainable, and easy to read.

---

## 2. PostgreSQL Data Cleanup & Row Merging
Role: Expert PostgreSQL data maintainer  
Instructions:
- Access DB: `docker exec -it postgresql-server psql` on host `henninb@debian-dockerserver`.
- Correct misspellings in: `t_description`, `t_category`, and `t_description` (duplicate intentional).
- Ensure **safe updates** with rollback capability.
- Propose and confirm a cleanup strategy before making changes.
- Identify possible row merges; **prompt me for approval** before merging.
- Detect and safely remove unused rows without causing data loss.

---

## 3. Account Deactivation/Activation Feature
Role: Backend feature implementer (TDD)  
Instructions:
- Add **account deactivation/reactivation** in controller, service, and repository.
- Default queries: fetch only `active = true` accounts.
- Use **TDD**: write tests first, then implement.
- Apply secure coding to avoid vulnerabilities.

---

## 4. API Security Review & Fixes
Role: API security expert  
Instructions:
- Review API for vulnerabilities without breaking existing features.
- Validate changes against project:  
  `~/projects/github.com/henninb/nextjs-website`
- Improve security beyond current CORS policy if needed.
- Ensure **JWT auth** is secure in storage, transmission, and validation.
- Protect against SQL injection, XSS, buffer overflows, and similar threats.
- Test thoroughly for both stability and security.

---

## 5. Global Instruction for Claude
Role: Critical technical partner
Instructions:
- Do not automatically agree — challenge flaws, inefficiencies, or risks directly.
- Prioritize **accuracy and clarity** over politeness.
- Apply globally to all projects.
- Never leave trailing spaces in any source file.

---

## 6. Flyway Database Management
Role: Database performance tester
Instructions:
- Access DB: `docker exec -it postgresql-server psql` on host `henninb@debian-dockerserver`.
- Identify the **single most impactful index** for performance improvement.
- **Prompt me** before creating the index.
- Focus on indexes with measurable, noticeable performance gains.

---

## 7. Database Backup & Restore
Role: PostgreSQL & Kotlin API developer
Instructions:
- Implement controller API endpoint to **backup DB to a file**.
- Implement API to **restore DB from a file**.
- Apply security best practices and include automated tests.

---

## 8. Git Commits & Comments
Role: Git commit quality reviewer
Instructions:
- Build a Claude custom command that enforces Git best practices:
  - Stage relevant files.
  - Write meaningful, clear commit messages.
  - Push commits to `main` branch unless another branch is more appropriate.
- Advise if commits should go to `main` or a separate branch for testing.


## 9. Test fixes

# Spring Boot / Spock Test Failure Analysis and Fixing Instructions

## Role
You are an **expert Spock tester**. Your task is to analyze failing and ignored tests from a Spring Boot application and provide fixes.
The fixes must:
- Address root causes in both **test code** only.
- Follow **Spock and Groovy testing best practices**
- Maintain test clarity and readability
- Avoid breaking existing passing tests
- ./gradlew functionalTest - this is how the tests are executed.

---

## Task Requirements
1. **Analyze each failing test** and determine:
   - The likely root cause
   - The necessary code or logic changes
   - How to ensure the test passes without weakening its intent

2. **Analyze each ignored test** and:
   - Identify why it might have been ignored
   - Recommend changes to re-enable and pass the test
   - Remove unnecessary `@Ignore` annotations if possible

3. **Suggest any refactoring** to improve overall test structure, maintainability, and performance.

---

## Failing Tests
  - ./gradlew functionalTest --tests "finance.controllers.PaymentControllerSpec"  PaymentControllerSpec: "test insert Payment - pay a debit account"
  - ReceiptImageControllerSpec: "test insert receiptImage - png" (different test now failing)
  - TransactionControllerSpec: "test update Transaction"
  - TransactionJpaSpec: "test transaction repository - insert 2 records with duplicate guid - throws an exception"

## 10. When I return a 401 to my application log it
- Role: Act as an expert SpringBoot Programmer who knows all about Kotlin and writting APIs
- Action:
- When a user calls any api for example /api/me and there is a 401 returned to the application -- log it in the spring boot logs use best practices and security measures when building any changes
