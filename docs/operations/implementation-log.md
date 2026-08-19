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

## 2026-08-19 23:08 KST — Persistence, idempotency, and stale approval

### Persistence

- RED: no organization-scoped JPA store or domain version restoration contract existed.
- The first GREEN attempt exposed a real schema mismatch: PostgreSQL `char(3)` currency versus JPA `varchar(3)`.
- Resolution: retained immutable `V001` history and added `V003__currency_varchar.sql` instead of rewriting an applied migration.
- Verified round trip: title, two items, KRW 2,580,000 total, DRAFT status, and version 0.

### Idempotent submission

- The first request reserves an organization-scoped `PROCUREMENT_SUBMIT` key and stores its request hash and serialized success result.
- Repeating the same key and hash returns the original result without a second submit or audit event.
- Reusing the key with a different hash returns `IDEMPOTENCY_KEY_REUSED`.
- Verified database counts: one idempotency record and one `REQUEST_SUBMITTED` audit event.

### Stale approval and child-row identity

- Contract RED: approval service, result, and conflict types did not exist.
- Functional RED: rebuilding a restored JPA aggregate generated a new approval-step ID and violated `uq_approval_step_sequence`.
- Resolution: persisted aggregates now load their managed entity and update existing approval-step rows by sequence. New aggregate creation remains a separate path.
- A first approval succeeds; a repeated approval with the stale expected version returns `REQUEST_VERSION_CONFLICT`; exactly one `REQUEST_APPROVED` audit event remains.
- Evidence: `docs/incidents/evidence/INC-001/red.txt` and `green.txt`.

### Verification

- Fresh full result after Task 5: 20 tests, 0 failures, 0 errors.
- Public-data scan found no personal email, local home path, private key, authorization token, revenue tracker, or candidate packet in tracked source.

## 2026-08-19 23:25 KST — Procurement API and stable problems

### API surface

- `POST /api/procurement/requests`: creates a DRAFT from the authenticated organization and requester, recalculates the total, and returns 201 plus `Location`.
- `GET /api/procurement/requests/{id}`: reads only inside the authenticated organization scope.
- `POST /api/procurement/requests/{id}/submit`: uses a client idempotency key and a server-calculated canonical SHA-256.
- `POST /api/procurement/requests/{id}/approve`: derives actor and roles from the authenticated session and accepts only `expectedVersion` from the body.

### Stable problem contract

- Validation: 400 / `VALIDATION_FAILED` / sorted field errors.
- Hidden or missing request: 404 / `PROCUREMENT_NOT_FOUND`.
- Idempotency reuse: 409 / `IDEMPOTENCY_KEY_REUSED`.
- Stale approval: 409 / `REQUEST_VERSION_CONFLICT`.
- Invalid state transition: 422 with its domain code.
- Authentication: generic 401 / `INVALID_CREDENTIALS`.
- CSRF or access denial: JSON 403 / `CSRF_FAILED` or `ACCESS_DENIED`.
- Every tested error includes a generated `traceId`; the same value is also returned in `X-Trace-Id`.

### Problems reproduced and resolved

- Spring Boot's default validation Problem Detail took precedence over the application advice and omitted stable fields. `ApiProblemHandler` now has explicit highest precedence.
- A missing `items` field passed `@Size` because null is valid for that constraint, then caused a controller NPE. Adding `@NotNull` converts the case to the same stable validation response as an empty or invalid list.
- The initial API draft allowed the client to send `requestHash`. The server now computes the SHA-256 from the authenticated organization, operation, and request ID; the client supplies only the idempotency key.

### Verification

- Fresh full result after Task 6: 26 tests, 0 failures, 0 errors.
- Public-data scan found no personal email, local home path, private key, bearer token, revenue tracker, or candidate packet.

## 2026-08-19 23:38 KST — React procurement workflow

- Runtime: Node 20.20.2, npm 10.8.2, React 19.2.8, Vite 8.2.1.
- `npm audit`: 0 vulnerabilities at installation time.
- Visual direction: industrial editorial operations console using bundled IBM Plex Sans KR and IBM Plex Mono; warm paper surfaces, ink typography, restrained teal and orange states, no colored left-edge accent bars.
- Component RED/GREEN: purchase form total and payload boundary, organization login payload, and conflict reload while preserving an unsent review note.
- A real form-boundary bug was fixed: React Hook Form passed the submit event as a second callback argument; the component now forwards only the allowed DTO.
- Login and API client use session cookies plus CSRF bootstrap with `credentials: include`; no token persistence or browser auth storage exists.
- App routes: new procurement request and request detail. Draft creation navigates to the actual request URL; submit reloads the server state.
- Verification: 3 Vitest tests pass, Oxlint emits no warnings, TypeScript and Vite production build succeed.
- Generated Vite logo and hero assets were removed before commit.

## 2026-08-19 23:55 KST — Docker and browser verification

- Local tool gap: the Docker CLI lacked Compose; Homebrew `docker-compose` 5.5.0 was installed to execute the approved local verification plan.
- First backend image attempt failed because Temurin 17 Alpine had no Linux arm64 manifest. The Dockerfile now uses multi-architecture Temurin 17 Jammy build/runtime stages and runs as a non-root `flowops` user.
- Core services: PostgreSQL 16 Alpine, Spring Boot backend, and Nginx frontend.
- Demo data is isolated under `db/demo/V900__demo_accounts.sql` and included only when Docker sets the extra Flyway location.
- Browser RED: no server produced `ERR_CONNECTION_REFUSED`; the first live login then exposed a real raw-cookie/XOR CSRF mismatch.
- CSRF resolution: a regression test now sends the actual `XSRF-TOKEN` cookie value through `X-XSRF-TOKEN`; Spring uses an explicit raw `CsrfTokenRequestAttributeHandler` for the SPA.
- Session probe resolution: unauthenticated `GET /api/session` returns 204 instead of a noisy expected 401, while protected business APIs remain authenticated.
- Logout returns 204 and invalidates the session.
- Playwright GREEN: create, submit, log out, log in as another organization, and verify hidden request 404.
- Real desktop login, desktop request, and mobile request screenshots were captured with fictional data and zero browser console/page errors.
- Mobile table was reworked to prevent Korean words and currency values from breaking per character.

## 2026-08-20 00:02 KST — Public-readiness documentation and gates

- Added English and Korean repository introductions, explicit claim boundaries, local demo instructions, security policy, module map, ERD, and bilingual portfolio case studies.
- Expanded INC-001 into a full development-reproduced incident record with impact, reproduction, root cause, rejected alternatives, selected fix, regression, and limits.
- Added local CI workflow definition for wrapper validation, backend tests, frontend verification, Docker Compose E2E, and public-safety gates. Remote CI remains unverified until a real GitHub Actions run is observed.
- Added blocking checks for browser auth storage, personal emails, home paths, credential shapes, internal revenue workflow files, forbidden tracked artifacts, and the complete Git history.
- Current safety checks: `PASS browser-auth-storage`, `PASS public-safety`.
- License remains intentionally unselected; repository creation, public visibility, remote addition, and push remain exact external-action gates.
