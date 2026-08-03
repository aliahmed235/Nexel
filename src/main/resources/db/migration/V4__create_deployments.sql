-- One build of a project. A project has many deployments over time; the one
-- with is_current = true is the live version. Nothing here is written past
-- QUEUED until the build worker (phase 3.3) exists.
create table deployments
(
    id            bigserial primary key,
    project_id    bigint      not null references projects (id) on delete cascade,
    status        varchar(20) not null,
    commit_sha    varchar(64),
    is_current    boolean     not null default false,
    error_message text,
    created_at    timestamptz not null default now(),
    updated_at    timestamptz not null default now(),
    ready_at      timestamptz
);

create index idx_deployments_project_id on deployments (project_id);

-- At most one live deployment per project, enforced by the database. This is
-- what makes rollback safe later: flipping is_current can never produce two
-- live versions of the same project.
create unique index uq_deployments_current on deployments (project_id) where is_current;
