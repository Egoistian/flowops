create table organizations (
    id uuid primary key,
    name varchar(160) not null,
    status varchar(32) not null,
    created_at timestamptz not null default current_timestamp,
    constraint ck_organizations_status check (status in ('ACTIVE', 'SUSPENDED'))
);

create table users (
    id uuid primary key,
    organization_id uuid not null references organizations (id),
    email varchar(320) not null,
    display_name varchar(120) not null,
    password_hash varchar(100) not null,
    status varchar(32) not null,
    created_at timestamptz not null default current_timestamp,
    constraint uq_users_id_organization unique (id, organization_id),
    constraint ck_users_status check (status in ('ACTIVE', 'SUSPENDED'))
);

create unique index uq_users_org_email
    on users (organization_id, lower(email));

create table user_roles (
    user_id uuid not null,
    organization_id uuid not null,
    role varchar(32) not null,
    created_at timestamptz not null default current_timestamp,
    primary key (user_id, organization_id, role),
    constraint fk_user_roles_user
        foreign key (user_id, organization_id)
        references users (id, organization_id),
    constraint ck_user_roles_role
        check (role in ('REQUESTER', 'REVIEWER', 'MANAGER', 'BUDGET_OWNER', 'ADMIN'))
);

create table procurement_requests (
    id uuid primary key,
    organization_id uuid not null references organizations (id),
    requester_id uuid not null,
    title varchar(200) not null,
    purpose varchar(2000) not null,
    budget_code varchar(80) not null,
    total_amount numeric(19, 2) not null default 0,
    currency char(3) not null default 'KRW',
    status varchar(32) not null,
    version bigint not null default 0,
    submitted_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint uq_procurement_requests_id_organization unique (id, organization_id),
    constraint fk_procurement_requester
        foreign key (requester_id, organization_id)
        references users (id, organization_id),
    constraint ck_procurement_total_non_negative check (total_amount >= 0),
    constraint ck_procurement_currency check (currency = 'KRW'),
    constraint ck_procurement_status
        check (status in (
            'DRAFT', 'SUBMITTED', 'IN_REVIEW', 'CHANGES_REQUESTED',
            'REJECTED', 'APPROVED', 'CANCELLED', 'FULFILLING', 'COMPLETED'
        ))
);

create index ix_procurement_org_status_created
    on procurement_requests (organization_id, status, created_at desc);

create table procurement_items (
    id uuid primary key,
    organization_id uuid not null,
    request_id uuid not null,
    name varchar(200) not null,
    quantity integer not null,
    unit_price numeric(19, 2) not null,
    subtotal numeric(19, 2) not null,
    created_at timestamptz not null default current_timestamp,
    constraint fk_procurement_items_request
        foreign key (request_id, organization_id)
        references procurement_requests (id, organization_id)
        on delete cascade,
    constraint ck_procurement_item_quantity_positive check (quantity > 0),
    constraint ck_procurement_item_unit_price_non_negative check (unit_price >= 0),
    constraint ck_procurement_item_subtotal_non_negative check (subtotal >= 0)
);

create table approval_steps (
    id uuid primary key,
    organization_id uuid not null,
    request_id uuid not null,
    sequence_number integer not null,
    required_role varchar(32) not null,
    assigned_reviewer_id uuid,
    status varchar(32) not null,
    decision_reason varchar(1000),
    decided_at timestamptz,
    created_at timestamptz not null default current_timestamp,
    constraint uq_approval_step_sequence unique (organization_id, request_id, sequence_number),
    constraint fk_approval_steps_request
        foreign key (request_id, organization_id)
        references procurement_requests (id, organization_id)
        on delete cascade,
    constraint fk_approval_steps_reviewer
        foreign key (assigned_reviewer_id, organization_id)
        references users (id, organization_id),
    constraint ck_approval_step_sequence_positive check (sequence_number > 0),
    constraint ck_approval_step_role
        check (required_role in ('REVIEWER', 'MANAGER', 'BUDGET_OWNER')),
    constraint ck_approval_step_status
        check (status in ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'))
);

create table idempotency_records (
    id uuid primary key,
    organization_id uuid not null references organizations (id),
    operation varchar(80) not null,
    idempotency_key varchar(200) not null,
    request_hash char(64) not null,
    status varchar(32) not null,
    response_json jsonb,
    created_at timestamptz not null default current_timestamp,
    updated_at timestamptz not null default current_timestamp,
    constraint ck_idempotency_status check (status in ('PROCESSING', 'SUCCEEDED', 'FAILED'))
);

create unique index uq_idempotency_org_key
    on idempotency_records (organization_id, operation, idempotency_key);

create table audit_events (
    id uuid primary key,
    organization_id uuid not null references organizations (id),
    aggregate_type varchar(80) not null,
    aggregate_id uuid not null,
    event_type varchar(120) not null,
    actor_id uuid,
    payload jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default current_timestamp,
    constraint fk_audit_events_actor
        foreign key (actor_id, organization_id)
        references users (id, organization_id)
);

create index ix_audit_org_aggregate_created
    on audit_events (organization_id, aggregate_type, aggregate_id, created_at);
