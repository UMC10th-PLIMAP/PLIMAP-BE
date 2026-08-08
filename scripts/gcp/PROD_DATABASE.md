# Prod database bootstrap

Prod targets Cloud SQL PostgreSQL 17 and PostGIS 3.5.2. On 2026-08-06, newly created Cloud SQL PostgreSQL 18.4 instances returned `POSTGIS_AVAILABLE=false` from `pg_available_extensions` in both Enterprise (`db-custom-1-3840`) and Enterprise Plus (`db-perf-optimized-N-2`), so PostgreSQL 18 must not be used for Prod.

The final PostgreSQL 17 validation on 2026-08-06 reported PostGIS `default_version=3.6.0`, confirmed version `3.5.2` in `pg_available_extension_versions`, installed 3.5.2 explicitly, applied all 18 Flyway migrations, started the application, passed runtime CRUD, denied runtime DDL and extension creation, denied runtime Flyway-history access, and confirmed neither application role belongs to `cloudsqlsuperuser`. The temporary instance was deleted after the suite passed.

The accepted temporary validation retained evidence for `server_version`, `pg_available_extensions.default_version`, availability of version `3.5.2` in `pg_available_extension_versions`, installed `pg_extension.extversion`, Flyway history, application CRUD, DDL denial for `plimap_app`, extension-creation denial for non-admin roles, and absence of `cloudsqlsuperuser` membership. Cloud SQL maintenance releases can advance an extension's default version. Prod does not silently follow that default: the administrator bootstrap pins the initial PostGIS installation to `3.5.2`, and the gate stops if that version is unavailable or a different version is installed.

The permanent instance uses a direct private-IP JDBC connection from Cloud Run through Direct VPC egress. Set connector enforcement to `NOT_REQUIRED`. Do not copy the temporary proxy validation setting `REQUIRED` to Prod because it rejects direct database connections.

When the validation gate passes, use this order against the application database named by `DB_URL`:

1. Connect as `postgres` or another Cloud SQL `cloudsqlsuperuser` administrator.
2. Run [`bootstrap-prod-database.sql`](bootstrap-prod-database.sql). It creates `plimap_app` and `plimap_migrator` without repository-stored credentials, installs PostGIS, fails if either role belongs to `cloudsqlsuperuser`, and sets schema-level permissions.
3. Set the two role passwords outside the repository and store them in the separate `DB_*` and `FLYWAY_*` Secret Manager entries. Never put password values in either SQL file.
4. Connect as `plimap_migrator` and run [`configure-prod-database-grants.sql`](configure-prod-database-grants.sql) before the first Flyway run. This establishes default table and sequence privileges for `plimap_app`.
5. Start the application with `FLYWAY_USERNAME=plimap_migrator`. Flyway owns the objects it creates. `V202608061704__restrict_prod_flyway_history.sql` revokes all `plimap_app` privileges on the Flyway history table before startup completes.
6. Run `configure-prod-database-grants.sql` again only when an existing schema was created by a different owner, so its current tables and sequences receive the application grants. The script repeats the history-table revoke as defense in depth.

`postgres` in Cloud SQL is a `cloudsqlsuperuser`, but it is not a full PostgreSQL superuser. It cannot reliably grant privileges on tables owned by `plimap_migrator`; therefore table, sequence, and default-privilege grants belong in the migrator connection. Neither `plimap_app` nor `plimap_migrator` should retain `cloudsqlsuperuser`.

The existing `V1__init_schema.sql` keeps `CREATE EXTENSION IF NOT EXISTS postgis` for Local, Dev, and Test. In Prod, the administrator bootstrap must succeed before the first Flyway migration starts.
