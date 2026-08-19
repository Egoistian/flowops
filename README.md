# FlowOps

FlowOps is an independently developed operational approval portfolio built with Java 17, Spring Boot 3.5, PostgreSQL, React, and TypeScript. The first public-ready slice turns a procurement request into a traceable workflow with organization-scoped authentication, server-calculated totals, idempotent submission, optimistic version checks, stable API problems, and real browser verification.

> Portfolio boundary: this repository demonstrates independent implementation and local verification. It is not a paid client delivery, formal employment record, production customer system, payment platform, or claim of commercial scale.

![FlowOps login](docs/portfolio/screenshots/flowops-login-desktop.png)

![FlowOps procurement request](docs/portfolio/screenshots/flowops-request-desktop.png)

## What the first slice proves

- Session authentication with an organization key, email, BCrypt password, HttpOnly session cookie, and CSRF protection.
- Organization-scoped repository queries that return 404 without disclosing a foreign record.
- Procurement totals calculated from item quantity and unit price on the server.
- Amount-based approval plans with `REVIEWER`, `MANAGER`, and `BUDGET_OWNER` thresholds.
- Organization-scoped idempotency keys and server-calculated canonical request hashes.
- JPA optimistic versions translated to a stable `REQUEST_VERSION_CONFLICT` contract.
- Append-only audit events for draft creation, submission, and approval.
- Flyway schema evolution tested against PostgreSQL 16 rather than H2.
- React form, request sheet, conflict recovery UI, and responsive mobile layout.
- Docker Compose startup from an empty database and Playwright cross-organization verification.

## Architecture

```text
Browser
  └─ Nginx / React / TypeScript
       └─ same-origin /api proxy
            └─ Spring Boot modular monolith
                 ├─ identity + organization scope
                 ├─ procurement domain + application services
                 ├─ idempotency + audit
                 └─ JPA / Flyway
                      └─ PostgreSQL 16
```

The backend is a modular monolith. Domain rules do not depend on controllers or JPA. Application services own transaction boundaries. Infrastructure adapters map the domain to organization-scoped persistence. See [module map](docs/architecture/module-map.md) and [ERD](docs/architecture/erd.md).

## Quick start

> **Local demo only:** the Compose profile binds the web entry point to `127.0.0.1:4173`, loads public demo credentials, uses a local fallback database password, and disables the session cookie's `Secure` attribute so plain HTTP works on the same machine. Do not expose this profile to a LAN or the public internet. A real deployment must remove the demo Flyway location, use managed secrets, enforce HTTPS and secure cookies, and complete a production security review.

Requirements:

- Docker with Compose support
- or Java 17, Node 20.19+, and PostgreSQL 16 for manual execution

```bash
docker compose up --build --detach
curl -fsS http://127.0.0.1:4173/actuator/health
```

Open `http://127.0.0.1:4173`.

Local demonstration users are loaded only by the Docker demo Flyway location:

| Organization key | Email | Role |
|---|---|---|
| `northstar` | `requester@northstar.example.com` | `REQUESTER` |
| `northstar` | `reviewer@northstar.example.com` | `REVIEWER` |
| `acme` | `requester@acme.example.com` | `REQUESTER` |

The local-only demo password is `demo-password`. Do not enable `classpath:db/demo` in a production deployment.

## Verification

```bash
./scripts/verify-first-slice.sh
./scripts/check-local-demo-network.sh
./scripts/check-no-browser-auth-storage.sh
./scripts/check-public-safety.sh
```

The verification pipeline runs the local-network exposure gate, backend tests, frontend lint/tests/build, container health, and the Playwright journey. The public-safety gate checks current files and Git history for personal email addresses, home paths, common credential shapes, internal revenue-workflow files, logs, environment files, and browser traces.

## Stable errors

API failures use `application/problem+json` with a stable `code` and `traceId`. Covered cases include `VALIDATION_FAILED`, `INVALID_CREDENTIALS`, `CSRF_FAILED`, `PROCUREMENT_NOT_FOUND`, `IDEMPOTENCY_KEY_REUSED`, and `REQUEST_VERSION_CONFLICT`.

## Development-reproduced incident

[INC-001](docs/incidents/INC-001-concurrent-approval.md) documents a child-row identity failure discovered while implementing stale approval protection. The evidence separates the failing test, root cause, selected fix, passing regression, and remaining limits. It does not describe a customer production incident.

## Current limits

- Procurement is the only complete workflow in this milestone.
- Content publication and field-service modules are designed but not implemented in this slice.
- Demo credentials are for local verification only.
- The Compose profile is not a production deployment template: it intentionally uses local-only credentials, loopback HTTP, and `Secure=false` for the session cookie.
- There is no payment, refund, settlement, medical, industrial-device, or regulated-data integration.
- There is no public production deployment, user count, uptime record, client acceptance, contract, or revenue claim.
- The CI workflow is prepared locally but cannot be called remotely verified until a repository is published and an actual run is observed.

## Repository status and license

This project is licensed under the [MIT License](LICENSE). A public source repository proves code availability and reviewability only; it does not prove a production deployment, customer use, external acceptance, or realized revenue.

한국어 문서: [README.ko.md](README.ko.md)
