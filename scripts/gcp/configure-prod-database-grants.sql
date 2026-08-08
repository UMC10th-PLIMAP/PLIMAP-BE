-- Run this file as plimap_migrator against the target Prod application DB.
-- Run it once before the first Flyway migration and again after any existing
-- schema was created by a different owner.

GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA public
TO plimap_app;

GRANT USAGE, SELECT, UPDATE
ON ALL SEQUENCES IN SCHEMA public
TO plimap_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLES TO plimap_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
GRANT USAGE, SELECT, UPDATE
ON SEQUENCES TO plimap_app;

-- Flyway owns and updates its history table. The runtime role must not read or
-- modify migration metadata. The final Migration performs the same revoke
-- before startup completes; this block is defense in depth for existing DBs.
DO $$
BEGIN
    IF to_regclass('public.flyway_schema_history') IS NOT NULL THEN
        EXECUTE
            'REVOKE ALL PRIVILEGES ON TABLE public.flyway_schema_history FROM plimap_app';
    END IF;
END
$$;
