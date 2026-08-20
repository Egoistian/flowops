# FlowOps — Organization-Scoped Procurement Approval System

FlowOps is an independent Spring Boot and React portfolio that treats procurement as a stateful, organization-scoped workflow rather than a CRUD demo.

The backend uses Java 17, Spring Boot 3.5, PostgreSQL 16, Flyway, JPA optimistic versions, session authentication, CSRF, idempotency records, and audit events. The React client creates a request from allowed business inputs only; organization, actor, status, and totals come from the authenticated session and server rules.

The implementation records development-reproduced failures, including a JPA/schema currency mismatch, stopped Testcontainer connections caused by context caching, default demo credentials in base migrations, duplicate approval-step rows, client-controlled idempotency hashes, and SPA raw-cookie CSRF mismatch. Each fix is tied to a focused regression and the full verification path.

The locally verified boundary includes backend integration tests, frontend component tests, lint, TypeScript production build, empty-database Docker startup, health checks, Chromium E2E, cross-organization 404 behavior, and desktop/mobile screenshots. It does not claim paid-client delivery, production traffic, security certification, a contract, payment, or revenue.

## CI portability failure found during publication

| Current status | Result |
|---|---|
| Application and browser E2E | Passed from the first remote run |
| Customer or production impact | None |
| Failed boundary | CI safety-script command portability |
| Remote CI after the fix | Two consecutive passes |
| Public `main` | Green |

![FlowOps CI recovery timeline](screenshots/flowops-ci-recovery-1600x1200.png)

The first GitHub Actions run failed after the locally green repository was published. Backend tests, frontend verification, Docker startup, and Playwright E2E had already passed on the Ubuntu runner. Only the final public-safety step failed. Its first actionable error was `docker-compose: command not found`: the local Mac used the standalone command, while GitHub exposed Compose as the `docker compose` plugin.

### Reasoning after the failure

I did not change the procurement application or the E2E flow. The failing boundary was a shell safety gate that ran after those components. A later JSON parser error was secondary because the missing command produced no configuration output. Installing another Compose binary in CI would have hidden the script's environment assumption instead of correcting it.

### Fix and regression

The gate now prefers `docker compose`, falls back to `docker-compose`, and fails explicitly when neither exists. A controlled plugin-only CLI fixture runs the real gate with the standalone binary removed from `PATH`. This checks behavior rather than grepping for a command string.

Two consecutive remote workflows passed after the change. On the final public `main`, 28 backend tests, 3 frontend tests, Compose E2E, cross-organization browser verification, and every public-safety check succeeded. [INC-002](../incidents/INC-002-compose-cli-portability.md) records the error chain, rejected alternatives, selected fix, regression, and remaining limits.

### Reflection

A local green run proves a specific code-and-environment pair. It does not prove the same repository on a different runner. The useful outcome was not the command substitution itself; it was turning two installation shapes into an explicit compatibility contract with a test that fails when the contract is broken.
