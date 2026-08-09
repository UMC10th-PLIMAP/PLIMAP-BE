-- In Prod, default privileges granted by plimap_migrator also apply to the
-- Flyway history table. Revoke runtime access before application startup
-- completes. Local, Dev, and Test do not create the Prod-only role, so this is
-- intentionally a no-op there.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'plimap_app')
        AND to_regclass('public.flyway_schema_history') IS NOT NULL THEN
        EXECUTE
            'REVOKE ALL PRIVILEGES ON TABLE public.flyway_schema_history FROM plimap_app';
    END IF;
END
$$;
