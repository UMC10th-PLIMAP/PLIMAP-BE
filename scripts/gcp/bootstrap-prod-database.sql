-- Run this file against the target Prod application database as postgres or
-- another Cloud SQL account with the cloudsqlsuperuser role.
--
-- Create the built-in users and set their passwords separately. Do not put
-- credentials in this file or in the repository:
--   plimap_app      <- DB_USERNAME / DB_PASSWORD
--   plimap_migrator <- FLYWAY_USERNAME / FLYWAY_PASSWORD
--
-- This file is database-scoped. Run it against the database named in DB_URL,
-- not only against the default postgres database.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'plimap_app') THEN
        CREATE ROLE plimap_app LOGIN;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'plimap_migrator') THEN
        CREATE ROLE plimap_migrator LOGIN;
    END IF;
END
$$;

-- Cloud SQL can advance the default extension version independently of the approved Prod baseline.
-- Pin the initial installation; V1 remains portable because its IF NOT EXISTS statement sees this administrator-owned extension.
CREATE EXTENSION IF NOT EXISTS postgis VERSION '3.5.2';

DO $$
BEGIN
    IF pg_has_role('plimap_app', 'cloudsqlsuperuser', 'MEMBER') THEN
        RAISE EXCEPTION 'plimap_app must not be a member of cloudsqlsuperuser';
    END IF;
    IF pg_has_role('plimap_migrator', 'cloudsqlsuperuser', 'MEMBER') THEN
        RAISE EXCEPTION 'plimap_migrator must not be a member of cloudsqlsuperuser';
    END IF;
END
$$;

DO $$
BEGIN
    EXECUTE format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', current_database());
    EXECUTE format(
        'GRANT CONNECT ON DATABASE %I TO plimap_app, plimap_migrator',
        current_database()
    );
END
$$;

REVOKE CREATE ON SCHEMA public FROM PUBLIC;

GRANT USAGE ON SCHEMA public TO plimap_app;
GRANT USAGE, CREATE ON SCHEMA public TO plimap_migrator;

-- Table and sequence grants must be run by plimap_migrator after Flyway,
-- because that role owns the objects created by its migrations. Use the
-- companion configure-prod-database-grants.sql file for that step.
