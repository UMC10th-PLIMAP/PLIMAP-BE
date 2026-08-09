-- Run this file as plimap_migrator against the target Prod application DB.
-- Run it once before the first Flyway migration. ALTER DEFAULT PRIVILEGES
-- affects only objects subsequently created by the current role.

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLES TO plimap_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE
ON SEQUENCES TO plimap_app;
