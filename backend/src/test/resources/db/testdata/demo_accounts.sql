insert into organizations (id, organization_key, name, status)
values
    ('11111111-1111-1111-1111-111111111111', 'northstar', 'Northstar Studio', 'ACTIVE'),
    ('22222222-2222-2222-2222-222222222222', 'acme', 'Acme Operations', 'ACTIVE')
on conflict do nothing;

insert into users (id, organization_id, email, display_name, password_hash, status)
values
    (
        '11111111-1111-1111-1111-111111111101',
        '11111111-1111-1111-1111-111111111111',
        'requester@northstar.example.com',
        'Min Park',
        '$2y$12$RprayAULy9LHxK1qj29gN.zj0.S.XyD0GV7JT0vEXXRwoatn/Vybu',
        'ACTIVE'
    ),
    (
        '11111111-1111-1111-1111-111111111102',
        '11111111-1111-1111-1111-111111111111',
        'reviewer@northstar.example.com',
        'Jin Lee',
        '$2y$12$RprayAULy9LHxK1qj29gN.zj0.S.XyD0GV7JT0vEXXRwoatn/Vybu',
        'ACTIVE'
    ),
    (
        '22222222-2222-2222-2222-222222222201',
        '22222222-2222-2222-2222-222222222222',
        'requester@acme.example.com',
        'Alex Kim',
        '$2y$12$RprayAULy9LHxK1qj29gN.zj0.S.XyD0GV7JT0vEXXRwoatn/Vybu',
        'ACTIVE'
    )
on conflict do nothing;

insert into user_roles (user_id, organization_id, role)
values
    (
        '11111111-1111-1111-1111-111111111101',
        '11111111-1111-1111-1111-111111111111',
        'REQUESTER'
    ),
    (
        '11111111-1111-1111-1111-111111111102',
        '11111111-1111-1111-1111-111111111111',
        'REVIEWER'
    ),
    (
        '22222222-2222-2222-2222-222222222201',
        '22222222-2222-2222-2222-222222222222',
        'REQUESTER'
    )
on conflict do nothing;
