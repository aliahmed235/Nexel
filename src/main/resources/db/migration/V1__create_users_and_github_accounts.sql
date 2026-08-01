-- Identity. Stable, never contains credentials.
create table users
(
    id           bigserial primary key,
    github_id    bigint       not null unique,
    github_login varchar(255) not null,
    email        varchar(320),
    name         varchar(255),
    avatar_url   varchar(512),
    created_at   timestamptz  not null default now(),
    updated_at   timestamptz  not null default now()
);

create index idx_users_github_login on users (github_login);

create table github_accounts
(
    id               bigserial primary key,
    user_id          bigint      not null unique references users (id) on delete cascade,
    access_token_enc text        not null,
    token_type       varchar(64),
    scope            varchar(512),
    connected_at     timestamptz not null default now(),
    updated_at       timestamptz not null default now()
);
