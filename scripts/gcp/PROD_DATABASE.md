# Prod 데이터베이스 운영 및 재구축 초기 구성

영구 Prod 데이터베이스는 Cloud SQL PostgreSQL 17과 PostGIS 3.5.2로 운영합니다. 2026-08-06 임시 호환성 검증에서 PostgreSQL 18은 PostGIS를 제공하지 않았으므로 승인된 기준에 포함하지 않았습니다. 다만 이 결과를 공급자의 영구 정책으로 간주하지 않고, 향후 PostgreSQL 주 버전을 변경하기 전에 제공 여부를 다시 검증합니다.

2026-08-06 최종 PostgreSQL 17 검증에서는 PostGIS `default_version=3.6.0`을 확인하고, `pg_available_extension_versions`에서 `3.5.2`를 확인해 해당 버전을 명시적으로 설치했습니다. 당시 존재한 Flyway Migration 18개를 모두 적용하고 애플리케이션 기동과 런타임 CRUD를 검증했으며, 런타임 DDL·확장 생성·Flyway 이력 접근이 거부되는지와 두 애플리케이션 역할이 `cloudsqlsuperuser`에 속하지 않는지도 확인했습니다. 검증을 마친 임시 인스턴스는 삭제했습니다.

승인된 임시 검증에서는 `server_version`, `pg_available_extensions.default_version`, `pg_available_extension_versions`의 `3.5.2` 제공 여부, `pg_extension.extversion`, Flyway 이력, 애플리케이션 CRUD, `plimap_app`의 DDL 거부, 비관리자 역할의 확장 생성 거부와 `cloudsqlsuperuser` 미소속을 증빙으로 남겼습니다. Cloud SQL 유지보수 릴리스에 따라 확장의 기본 버전이 바뀔 수 있지만 Prod는 이를 자동으로 따라가지 않습니다. 관리자 초기 구성은 최초 PostGIS 설치 버전을 `3.5.2`로 고정하며, 해당 버전을 사용할 수 없거나 다른 버전이 설치되면 검증을 중단합니다.

영구 인스턴스는 Cloud Run의 Direct VPC egress를 통해 private IP 기반 JDBC로 직접 연결합니다. Connector enforcement는 `NOT_REQUIRED`로 유지합니다. 임시 프록시 검증에 사용한 `REQUIRED`는 직접 DB 연결을 거부하므로 Prod에 적용하지 않습니다.

다음 초기 구성 순서는 승인된 신규 인스턴스 또는 재해 복구 재구축에만 사용합니다. 현재 운영 중인 데이터베이스의 상태를 확인하려는 목적으로 다시 실행하지 않습니다.

1. `postgres` 또는 다른 Cloud SQL `cloudsqlsuperuser` 관리자 계정으로 연결합니다.
2. [`bootstrap-prod-database.sql`](bootstrap-prod-database.sql)을 실행합니다. 이 스크립트는 저장소에 자격 증명을 남기지 않고 `plimap_app`과 `plimap_migrator`를 생성하고 PostGIS를 설치하며, 두 역할 중 하나라도 `cloudsqlsuperuser`에 속하면 실패하고 스키마 단위 권한을 설정합니다.
3. 두 역할의 비밀번호는 저장소 밖에서 설정하고 각각의 `DB_*`, `FLYWAY_*` Secret Manager 항목에 저장합니다. 두 SQL 파일에는 비밀번호 값을 넣지 않습니다.
4. `plimap_migrator`로 연결해 재구축한 DB에서 Flyway를 실행하기 전에 [`configure-prod-database-grants.sql`](configure-prod-database-grants.sql)을 실행합니다. 이후 `plimap_migrator`가 생성하는 객체에 적용할 기본 테이블 DML 권한과 시퀀스 `USAGE` 권한을 설정합니다.
5. 데이터베이스에 다른 역할이 소유한 기존 객체가 있다면 각 소유자로 연결하거나 `SET ROLE`한 뒤 [`grant-prod-database-existing-objects.sql`](grant-prod-database-existing-objects.sql)을 각각 실행합니다. 이 스크립트는 `current_user`가 소유한 객체만 변경하고, 시퀀스에는 `SELECT`나 `UPDATE` 없이 `USAGE`만 부여하며, 소유 중인 Flyway 이력 테이블에 대한 runtime 접근을 회수합니다.
6. `FLYWAY_USERNAME=plimap_migrator`로 애플리케이션을 시작합니다. Flyway가 생성한 객체는 Flyway 계정이 소유합니다. `V202608061704__restrict_prod_flyway_history.sql`은 기동이 끝나기 전에 Flyway 이력 테이블에 대한 `plimap_app`의 모든 권한을 회수합니다.

Cloud SQL의 `postgres`는 `cloudsqlsuperuser`이지만 완전한 PostgreSQL 슈퍼유저는 아닙니다. 다른 역할이 소유한 테이블에 권한을 안정적으로 부여할 수 없으므로 기본 권한은 `plimap_migrator` 연결에서 설정하고, 기존 객체의 권한은 각 객체 소유자로 실행해야 합니다. `plimap_app`과 `plimap_migrator`에는 `cloudsqlsuperuser`를 부여하지 않습니다.

기존 `V1__init_schema.sql`은 Local, Dev, Test를 위해 `CREATE EXTENSION IF NOT EXISTS postgis`를 유지합니다. 신규 또는 재구축 Prod 데이터베이스에서는 Flyway를 시작하기 전에 관리자 초기 구성이 성공해야 합니다.
