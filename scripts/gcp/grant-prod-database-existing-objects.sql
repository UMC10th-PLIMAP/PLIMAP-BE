-- Run this file separately as each owner of existing Prod schema objects.
-- For multiple owners, connect as or SET ROLE to each owner and rerun it.
-- Only objects owned by current_user are changed.

DO $$
DECLARE
    database_object record;
BEGIN
    FOR database_object IN
        SELECT n.nspname AS schema_name, c.relname AS object_name
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
          AND c.relowner = (
              SELECT oid
              FROM pg_roles
              WHERE rolname = current_user
          )
          AND c.relkind IN ('r', 'p', 'v', 'm', 'f')
    LOOP
        EXECUTE format(
            'GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE %I.%I TO plimap_app',
            database_object.schema_name,
            database_object.object_name
        );
    END LOOP;

    FOR database_object IN
        SELECT n.nspname AS schema_name, c.relname AS object_name
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
          AND c.relowner = (
              SELECT oid
              FROM pg_roles
              WHERE rolname = current_user
          )
          AND c.relkind = 'S'
    LOOP
        EXECUTE format(
            'GRANT USAGE ON SEQUENCE %I.%I TO plimap_app',
            database_object.schema_name,
            database_object.object_name
        );
    END LOOP;

    IF EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
          AND c.relname = 'flyway_schema_history'
          AND c.relowner = (
              SELECT oid
              FROM pg_roles
              WHERE rolname = current_user
          )
    ) THEN
        REVOKE ALL PRIVILEGES
        ON TABLE public.flyway_schema_history
        FROM plimap_app;
    END IF;
END
$$;
