# Security Policy

FlowOps is an independent portfolio application and is not offered as a hosted production service in this milestone.

## Demonstration safety

- Docker demo users use fictional names and reserved example domains.
- Demo credentials live in a separate Flyway location and must not be enabled in production.
- Browser authentication uses a server session and CSRF cookie/header pair; authentication state is not stored in browser storage.
- Business records are queried with the authenticated organization identifier and record identifier together.
- Error bodies use stable codes and trace IDs without returning passwords, cookies, CSRF values, or foreign organization data.

## Reporting

Do not place secrets, personal data, identity documents, production database dumps, or customer material in a public issue. Until a public repository and security contact are explicitly configured, keep reports local to the repository owner.

## Unsupported claims

This project has not received a penetration test, compliance certification, security accreditation, or production incident-response validation. Automated tests and design controls are engineering evidence, not a security guarantee.
