-- The id of the GitHub push webhook registered for this project, so a disconnect
-- can remove it and an incoming push can be matched back to its project.
alter table projects
    add column github_hook_id bigint;
