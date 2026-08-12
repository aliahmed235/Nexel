-- Vercel's "Root Directory": the subfolder to build in for a monorepo (e.g. "Client").
-- Null means auto-detect (build the repo root, or the first subfolder with a build script).
alter table projects
    add column root_directory varchar(255);
