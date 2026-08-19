# FlowOps — Organization-Scoped Procurement Approval System

FlowOps is an independent Spring Boot and React portfolio that treats procurement as a stateful, organization-scoped workflow rather than a CRUD demo.

The backend uses Java 17, Spring Boot 3.5, PostgreSQL 16, Flyway, JPA optimistic versions, session authentication, CSRF, idempotency records, and audit events. The React client creates a request from allowed business inputs only; organization, actor, status, and totals come from the authenticated session and server rules.

The implementation records development-reproduced failures, including a JPA/schema currency mismatch, stopped Testcontainer connections caused by context caching, default demo credentials in base migrations, duplicate approval-step rows, client-controlled idempotency hashes, and SPA raw-cookie CSRF mismatch. Each fix is tied to a focused regression and the full verification path.

The locally verified boundary includes backend integration tests, frontend component tests, lint, TypeScript production build, empty-database Docker startup, health checks, Chromium E2E, cross-organization 404 behavior, and desktop/mobile screenshots. It does not claim paid-client delivery, production traffic, security certification, a contract, payment, or revenue.
