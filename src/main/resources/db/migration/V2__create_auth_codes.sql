-- Short-lived, single-use codes handed to the browser after a GitHub login.
-- The JWT itself is never put in a URL; the frontend trades one of these for it.
create table auth_codes
(
    id         bigserial primary key,
    code_hash  varchar(64) not null unique,
    user_id    bigint      not null references users (id) on delete cascade,
    expires_at timestamptz not null,
    used_at    timestamptz,
    created_at timestamptz not null default now()
);

-- Only the hash is stored, so a database dump does not yield usable codes.
create index idx_auth_codes_expires_at on auth_codes (expires_at);
