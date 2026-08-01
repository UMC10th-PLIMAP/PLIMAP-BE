# Database Development Guide

## Overview

이 문서는 PLIMAP API의 데이터베이스 개발 환경과 스키마 관리 원칙을 설명합니다.

## 환경별 구성

| 환경 | 데이터베이스 | 연결 방식 | 설정 주입 |
| --- | --- | --- | --- |
| Local | Docker Compose `postgis/postgis:18-3.6` | `localhost:5432` 직접 연결 | 기본값 또는 `.env` |
| Dev | Supabase PostgreSQL/PostGIS | SSL 기반 Transaction Pooler | GCP Secret Manager |
| Test | `postgis/postgis:18-3.6` Testcontainers | 테스트별 컨테이너 | 테스트 전용 설정 |

- 스키마 관리: Flyway
- ORM 검증: Hibernate `ddl-auto: validate`
- 동적 조회: QueryDSL 7.4.0

Hibernate는 테이블을 생성하거나   변경하지 않는다. Flyway가 스키마를 변경하고 Hibernate는 엔티티 매핑이 적용된 스키마와 일치하는지만 검증한다.

## 로컬 실행

기본 설정은 별도 파일 없이 동작한다. 값을 바꿀 때만 `.env.example`을 `.env`로 복사한다.

```bash
docker compose up -d
./gradlew bootRun --args='--spring.profiles.active=local'
```

Windows PowerShell에서는 다음 명령을 사용한다.

```powershell
docker compose up -d
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Docker Compose는 `.env`를 자동으로 읽는다. IDE나 Gradle에서 실행하는 Spring Boot 프로세스는 `.env`를 직접 읽지 않으므로 기본값이 아닌 값을 사용할 때는 같은 변수를 실행 환경에 설정해야 한다.

현재 상태는 다음 명령으로 확인할 수 있다.

```bash
docker compose exec postgres psql -U plimap -d plimap_local_db \
  -c "SELECT installed_rank, version, description, success FROM flyway_schema_history;"
```

## 로컬 DB 초기화

컨테이너만 종료하면 데이터는 `postgres-data` 볼륨에 남는다.

```bash
docker compose down
```

Migration을 빈 DB에서 다시 적용해야 할 때만 볼륨까지 삭제한다. 이 명령은 로컬 데이터를 모두 삭제한다.

```bash
docker compose down -v
docker compose up -d
```

그 다음 애플리케이션을 `local` 프로필로 실행하면 Flyway가 Migration을 처음부터 적용한다.

## Dev DB

### 연결 구조

Dev 애플리케이션은 Cloud Run에서 `dev` 프로필로 실행되며, Supabase PostgreSQL/PostGIS의 Transaction Pooler에 연결한다.

```text
Cloud Run
    → application-dev.yml
    → DB_URL / DB_USERNAME / DB_PASSWORD
    → Supabase Transaction Pooler
    → PostgreSQL/PostGIS
```

`DB_URL`은 SSL 연결을 사용하는 JDBC URL이며 비밀번호를 포함하지 않는다. 사용자 이름과 비밀번호는 `DB_USERNAME`, `DB_PASSWORD`로 분리한다. Transaction Pooler 호환성을 위해 JDBC 드라이버의 `prepareThreshold`를 `0`으로 설정한다.

### 설정 및 Secret 관리

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`는 GCP Secret Manager에서 Cloud Run의 새 revision에 주입한다. 실제 연결 호스트, 사용자 이름과 비밀번호는 저장소, 문서, 로그에 기록하지 않는다.

Secret ID, 연결 정보 형식과 값 교체 절차는 [Dev Secret 관리 문서](../scripts/gcp/SECRETS.md)를 따른다. Secret의 값을 변경한 경우 새 revision이 최신 활성 버전을 주입받도록 Dev를 다시 배포한다.

### Migration 및 스키마 검증

Dev에서도 공통 설정에 따라 애플리케이션 시작 시 Flyway가 아직 적용되지 않은 Migration을 실행한다. 이후 Hibernate의 `ddl-auto: validate`가 적용된 스키마와 엔티티 매핑이 일치하는지 검증한다.

`baseline-on-migrate`는 Local 프로필에만 적용하며 Dev의 기존 스키마를 자동으로 기준선 처리하지 않는다. Dev에 적용할 스키마 변경은 반드시 `src/main/resources/db/migration`의 새 Migration으로 작성한다.

### 운영 안전 규칙

- Dev DB는 여러 사용자가 공유하므로 `flyway clean`이나 스키마 초기화를 실행하지 않는다.
- 이미 적용된 Migration은 수정하거나 삭제하지 않고 더 높은 버전의 새 Migration으로 보완한다.
- 수동 DDL을 기준 상태로 삼지 않으며 Migration과 관련 엔티티 변경을 같은 작업에서 관리한다.
- 파괴적 DDL과 대량 데이터 변경은 아래의 확장-전환-정리 절차에 따라 나눈다.
- Migration 적용 상태는 권한이 있는 관리 연결에서 `flyway_schema_history`를 조회해 확인한다.

## Flyway Migration 작성 규칙

- 위치: `src/main/resources/db/migration`
- 초기 스키마: `V1__init_schema.sql`
- 후속 파일: `VyyyyMMddHHmm__snake_case_description.sql`
- 예시: `V202607081530__add_member_last_login_at.sql`
- 한 파일은 하나의 논리적 스키마 변경만 담당한다.
- Migration과 관련 엔티티 변경은 같은 PR에서 함께 검증한다.
- 이미 `develop`에 병합된 Migration은 수정하거나 삭제하지 않는다.
- 적용된 변경을 고칠 때는 더 높은 버전의 새 Migration을 추가한다.
- 공유 DB에서 `flyway clean`을 사용하지 않는다.

여러 도메인 작업이 동시에 진행되므로 후속 버전에는 분 단위 타임스탬프를 사용한다. 파일명이 충돌하면 최종 커밋 전에 더 늦은 시각으로 버전을 변경한다.

### 실데이터가 있는 테이블 변경

파괴적 DDL은 한 번에 실행하지 않고 확장-전환-정리 순서로 나눈다.

1. nullable 컬럼이나 새 테이블을 먼저 추가한다.
2. 애플리케이션을 새 구조와 기존 구조가 함께 동작하도록 배포한다.
3. 기존 데이터를 backfill하고 검증한다.
4. 다음 배포에서 `NOT NULL` 같은 제약을 강화한다.
5. 더 이상 사용하지 않는 컬럼이나 테이블은 별도의 후속 Migration에서 제거한다.

`DROP TABLE`, `DROP COLUMN`, `TRUNCATE`, 대량 `UPDATE`, 컬럼 타입 변경은 데이터 손실과 잠금 영향을 별도로 검토해야 한다.

## QueryDSL 빌드 설정

Q 타입은 다음 Gradle 컴파일 경로에 생성된다.

```text
build/generated/sources/annotationProcessor/java/main
build/generated/sources/annotationProcessor/java/test
```

`build/`는 Git 추적 대상이 아니며 `gradlew clean`으로 함께 삭제된다. 생성된 Q 타입을 소스 디렉터리로 복사하거나 커밋하지 않는다.

Query Repository의 구조와 책임은 [아키텍처 문서](ARCHITECTURE.md#repository-query), 구현 및 네이밍 규칙은 [코드 스타일 문서](CODE_STYLE.md#repository-layer)를 따른다.

## 테스트 및 CI

DB가 필요한 테스트는 H2 대신 PostGIS Testcontainers를 사용한다. 테스트 컨테이너에는 운영 코드와 동일한 Flyway Migration이 적용된다.

```bash
./gradlew clean build
```

로컬에서 위 명령을 실행하려면 Docker가 실행 중이어야 한다. GitHub Actions의 `build` 작업은 `./gradlew build --no-daemon`을 실행하며, Migration, PostGIS, JPA Context와 QueryDSL 조회를 함께 검증한다.
