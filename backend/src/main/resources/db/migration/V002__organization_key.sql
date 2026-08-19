alter table organizations
    add column organization_key varchar(80);

alter table organizations
    add constraint uq_organizations_key unique (organization_key);

alter table organizations
    alter column organization_key set not null;
