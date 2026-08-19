# FlowOps First-Slice ERD

```mermaid
erDiagram
  ORGANIZATIONS ||--o{ USERS : contains
  USERS ||--o{ USER_ROLES : has
  ORGANIZATIONS ||--o{ PROCUREMENT_REQUESTS : owns
  USERS ||--o{ PROCUREMENT_REQUESTS : requests
  PROCUREMENT_REQUESTS ||--|{ PROCUREMENT_ITEMS : contains
  PROCUREMENT_REQUESTS ||--o{ APPROVAL_STEPS : plans
  ORGANIZATIONS ||--o{ IDEMPOTENCY_RECORDS : scopes
  ORGANIZATIONS ||--o{ AUDIT_EVENTS : scopes

  ORGANIZATIONS {
    uuid id PK
    varchar organization_key UK
    varchar name
    varchar status
  }
  USERS {
    uuid id PK
    uuid organization_id FK
    varchar email
    varchar password_hash
    varchar status
  }
  PROCUREMENT_REQUESTS {
    uuid id PK
    uuid organization_id FK
    uuid requester_id FK
    numeric total_amount
    varchar currency
    varchar status
    bigint version
  }
  PROCUREMENT_ITEMS {
    uuid id PK
    uuid organization_id FK
    uuid request_id FK
    integer quantity
    numeric unit_price
    numeric subtotal
  }
  APPROVAL_STEPS {
    uuid id PK
    uuid organization_id FK
    uuid request_id FK
    integer sequence_number
    varchar required_role
    varchar status
  }
  IDEMPOTENCY_RECORDS {
    uuid id PK
    uuid organization_id FK
    varchar operation
    varchar idempotency_key
    char request_hash
    jsonb response_json
  }
  AUDIT_EVENTS {
    uuid id PK
    uuid organization_id FK
    uuid aggregate_id
    varchar event_type
    uuid actor_id FK
    jsonb payload
  }
```

Composite foreign keys bind users, requests, items, approval steps, and actors to the same organization. The application does not rely on UI filtering as a tenant boundary.
