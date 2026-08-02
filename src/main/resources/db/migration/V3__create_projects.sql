create table projects
(
    id             bigserial primary key,
    user_id        bigint       not null references users (id) on delete cascade,
    github_repo_id bigint       not null,
    repo_full_name varchar(255) not null,
    default_branch varchar(255) not null,
    subdomain      varchar(255) not null unique,
    framework      varchar(64),
    created_at     timestamptz  not null default now(),
    updated_at     timestamptz  not null default now(),
    constraint uq_projects_user_repo unique (user_id, github_repo_id)
);

create index idx_projects_user_id on projects (user_id);
