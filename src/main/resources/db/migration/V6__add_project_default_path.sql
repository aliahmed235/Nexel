-- Optional landing path appended to a site's URL, e.g. "products" →
-- .../sites/<subdomain>/products. Null/blank keeps the URL at the root "/".
alter table projects
    add column default_path varchar(255);
