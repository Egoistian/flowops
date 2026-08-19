# FlowOps Implementation Log

## 2026-08-19 22:35 KST — Build bootstrap

- Java runtime: OpenJDK 17.0.18.
- Spring Boot line: 3.5.16.
- Gradle distribution: 8.14.3 binary distribution.
- Distribution URL: `https://services.gradle.org/distributions/gradle-8.14.3-bin.zip`.
- Published SHA-256: `bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531`.
- Downloaded SHA-256: `bd71102213493060956ec229d946beee57158dbd89d0e62b91bca0fa2c5f3531`.
- Integrity result: exact match.
- Spring Initializr metadata was checked first. Its live generator exposed only Spring Boot 4.0/4.1, so it was not used to generate or mix Boot 4 source into this Boot 3.5 project.
- Wrapper source: generated locally with the verified official Gradle 8.14.3 distribution.

## 2026-08-19 22:52 KST — First backend execution batch

### Task 1 — executable baseline

- RED: `FlowOpsApplicationTest` failed compilation because `FlowOpsApplication` did not exist.
- GREEN: the focused test and full test task passed after adding only the Spring Boot entry class.
- Commit: `8d3acda build: bootstrap FlowOps backend`.

### Task 2 — PostgreSQL schema

- Docker runtime: local Colima, Docker 29.5.2, Linux arm64.
- Database test image: `postgres:16-alpine`.
- RED: a fresh database contained zero of the four required representative tables.
- GREEN: Flyway applied `V001__identity_and_procurement.sql`; the focused migration test and full suite passed.
- Schema includes organization-scoped foreign keys, monetary and state checks, idempotency uniqueness, and audit indexes.
- Commit: `3d13f9e feat: add PostgreSQL schema baseline`.

### Task 3 — session security and organization isolation

- The original email-only login plan was corrected because email is unique only inside an organization. Login now requires `organizationKey + email + password`.
- RED/GREEN cases: successful organization-scoped login, generic invalid-password 401, authenticated session readback without email/password, current-organization read with foreign-organization hiding, and base migration without default credentials.
- The CSRF regression test was verified by temporarily disabling CSRF, observing the intended failure, restoring the repository configuration, and observing the passing result. The disabled state was never committed.
- A full-suite-only failure exposed a cached Spring DataSource pointing at a stopped per-class container. The test base now keeps one PostgreSQL container alive for the JVM and allows Ryuk to clean it at exit.
- Demo accounts were removed from production Flyway migrations. They now exist only in idempotent test fixture SQL and are deleted after each authentication test.
- Fresh full result: 9 tests, 0 failures, 0 errors.
- Commit: `a6a22fc feat: add organization-scoped session security`.

### Boundaries

- All identities, organizations, and addresses are fictional and use reserved example domains.
- No external repository, remote, upload, or push exists.
- No payment, settlement, platform-account, candidate, revenue, KYC, or personal data was added.

## 2026-08-19 22:56 KST — Procurement domain rules

- RED: procurement tests failed compilation because no money, item, request, approval-policy, or workflow types existed.
- GREEN: server-calculated KRW totals, exact approval thresholds, item-required submission, self-approval blocking, role matching, and one-step-at-a-time approval now pass without Spring or a database.
- Thresholds: below KRW 1,000,000 requires `REVIEWER`; KRW 1,000,000–4,999,999 adds `MANAGER`; KRW 5,000,000 and above adds `BUDGET_OWNER`.
- Fresh full result after Task 4: 16 tests, 0 failures, 0 errors.
- Commit: `d18f501 feat: model procurement approval rules`.
