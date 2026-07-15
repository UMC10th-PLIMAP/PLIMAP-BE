# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

PLIMAP은 음악과 장소를 연결해 지도 위에 기록하고 공유하는 서비스의 백엔드 API입니다. Java 21 + Spring Boot 4.1 기반이며, 위치 데이터를 다루기 위해 PostgreSQL + PostGIS(Hibernate Spatial)를 사용합니다.

## 자주 쓰는 명령어

로컬 실행은 Docker로 PostGIS를 먼저 띄우고 `local` 프로필로 애플리케이션을 실행합니다. Docker Compose는 `.env`를 자동으로 읽지만, IDE/Gradle로 직접 실행하는 Spring Boot 프로세스는 `.env`를 읽지 않으므로 기본값이 아닌 값을 쓸 때는 실행 환경에도 같은 변수를 설정해야 합니다.

```bash
docker compose up -d                                          # 로컬 PostGIS 기동
./gradlew bootRun --args='--spring.profiles.active=local'     # 앱 실행 (macOS/Linux)
.\gradlew.bat bootRun --args="--spring.profiles.active=local" # 앱 실행 (Windows)

./gradlew build              # 전체 빌드 + 테스트 (CI와 동일, Docker 필요)
./gradlew clean build        # 생성된 Q 타입까지 삭제 후 빌드
./gradlew test               # 테스트만 실행
./gradlew clean              # build/ 및 생성된 QueryDSL Q 타입 삭제

docker compose down          # 컨테이너 종료 (데이터는 volume에 유지)
docker compose down -v       # 데이터 volume까지 삭제 (Migration을 빈 DB에서 재적용할 때만)
```

단일 테스트 실행:

```bash
./gradlew test --tests "com.example.plimap.domain.pin.service.command.impl.PinCommandServiceImplTest"
./gradlew test --tests "*PinCommandServiceImplTest.핀_생성에_성공한다*"
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html` (기본적으로 `api-docs`/`swagger-ui`는 비활성이며 특정 프로필에서만 노출)

**테스트에는 Docker가 반드시 실행 중이어야 합니다.** DB가 필요한 테스트는 H2가 아니라 PostGIS Testcontainers를 사용하고, 운영 코드와 동일한 Flyway Migration을 컨테이너에 적용해 검증합니다. CI(`.github/workflows/ci.yml`)도 `./gradlew build`로 동일하게 검증합니다.

## Commit Message Rules

절대로 커밋 메시지에 다음을 포함하지 마세요:

🤖 Generated with Claude Code
Co-Authored-By: Claude
AI가 생성했다는 어떤 표시도 금지


## 아키텍처

**도메인 기반 계층형 구조(Package by Feature + Layered)** 를 사용합니다. 패키지를 먼저 도메인 단위로 나누고, 도메인 내부에서 계층을 분리합니다.

```
com.example.plimap
├── domain/          # auth, member, place, track, pin
│   └── {domain}/
│       ├── controller/        # REST 컨트롤러 (+ docs/ 에 Swagger용 인터페이스)
│       ├── converter/         # 복잡한 Entity↔DTO 변환
│       ├── dto/request, dto/response
│       ├── entity/            # JPA 엔티티
│       ├── enums/
│       ├── exception/         # 도메인 ErrorCode + Exception
│       ├── repository/        # 기본 CRUD (JpaRepository)
│       │   └── query/impl/    # QueryDSL 커스텀 조회
│       └── service/
│           ├── command/impl/  # 상태 변경 (생성·수정·삭제)
│           └── query/impl/    # 조회 (읽기)
└── global/          # apiPayload, config, entity, security, swagger, external
```

의존 방향: `Controller → Service → Repository → Entity`. 하위 계층은 상위 계층에 의존하지 않습니다.

### 부분 CQRS (핵심 규칙)

Service 계층은 **command**(상태 변경)와 **query**(조회)로 분리합니다. 별도 읽기 DB나 이벤트 소싱이 아니라 패키지·트랜잭션 수준의 책임 분리입니다.

- `CommandServiceImpl` → `@Transactional`
- `QueryServiceImpl` → `@Transactional(readOnly = true)`
- 단순 CRUD/단순 조건 조회는 Spring Data JPA 메서드, 동적 조건·복잡한 검색·페이징은 `repository/query`의 QueryDSL 구현체 사용
- **다른 도메인의 Repository를 직접 주입하지 않고**, 해당 도메인의 Service 인터페이스(주로 `QueryService`)를 통해 호출합니다. 도메인 간 순환 호출에 주의합니다.

### 데이터베이스 규칙

- **스키마는 Flyway가 관리하고 Hibernate는 `ddl-auto: validate`로 검증만 합니다.** Hibernate는 테이블을 생성/변경하지 않습니다.
- Migration 위치: `src/main/resources/db/migration`. 초기 스키마 `V1__init_schema.sql`, 후속은 `VyyyyMMddHHmm__snake_case.sql`(분 단위 타임스탬프로 파일명 충돌 방지). 엔티티 변경과 Migration은 같은 PR에서 함께 검증합니다.
- **이미 `develop`에 병합된 Migration은 절대 수정/삭제하지 않고**, 더 높은 버전의 새 Migration을 추가합니다. 공유 DB에서 `flyway clean` 금지.
- 시간 필드는 PostgreSQL `TIMESTAMPTZ`와 맞춰 Java `Instant`를 사용하고, `createdAt`/`updatedAt`은 JPA Auditing으로 자동 기록합니다.
- 모든 엔티티는 `BaseEntity`(createdAt/updatedAt)를 상속합니다. 삭제 이력 보존이 필요한 엔티티만 `SoftDeleteEntity`(deletedAt)를 상속합니다. **Soft Delete 대상: `Member`, `Place`, `PlaceTrack`, `Pin`**.
- Soft Delete 대상에는 `repository.delete()`/`deleteById()`를 쓰지 않고 `entity.delete()` / `restore()`를 호출합니다. 일반 조회는 `deletedAt IS NULL` 조건을 적용합니다.
- 생성된 QueryDSL Q 타입은 `build/generated/...`에 만들어지며 커밋하지 않습니다.

### 인증/보안

- `global/security` — JWT 발급/검증(`JwtUtil`, `JwtAuthFilter`), Bearer 토큰 매칭(`BearerTokenRequestMatcher`), CSRF 쿠키(`CsrfCookieFilter`), 보안 설정(`SecurityConfig`).
- 소셜 로그인(Kakao·Google OAuth2)은 `domain/auth`에서 처리하며, 성공 시 `OAuthSuccessHandler`에서 JWT를 발급합니다.
- 공통 응답/CORS/쿠키 설정은 프로필(`application-{local,dev,prod}.yml`)로 분리되어 있으므로 인증·CORS·CSRF·쿠키를 건드릴 때는 세 프로필 간 정합성을 함께 확인합니다.

## 코드 컨벤션

- **API 응답은 항상 `ApiResponse<T>`** 로 감쌉니다(`isSuccess`, `code`, `message`, `result`). 데이터가 없으면 `result`는 `null`, 타입은 `ApiResponse<Void>`. HTTP 상태는 SuccessCode/ErrorCode의 `HttpStatus`를 `ResponseEntity`에 설정합니다.
- **예외 처리**: 공통 코드/전역 핸들러는 `global/apiPayload`, 도메인 에러 코드·예외는 각 도메인 `exception` 패키지. 도메인 예외는 `BusinessException`을 상속하고 도메인 ErrorCode를 전달하며, `GlobalExceptionHandler`가 응답을 만듭니다. Controller에서 try-catch로 비즈니스 예외를 직접 처리하지 않습니다. 외부 API 예외는 내부 예외로 변환해 던집니다.
- **DTO는 `record`** 사용, Request/Response 분리. 단순 변환은 Response DTO의 `from()`, 복잡한 변환만 `converter` 패키지. Entity를 응답으로 직접 노출하지 않습니다.
- **Entity**: `@Getter` + `@NoArgsConstructor(access = PROTECTED)`만 기본 허용. `@Setter`/`@Data` 금지, 클래스 레벨 `@Builder` 금지(private 생성자에만 `@Builder` + 정적 팩토리 `create(...)`). 상태 변경은 의미 있는 메서드로, 연관관계는 `LAZY`, Enum은 `@Enumerated(EnumType.STRING)`.
- **네이밍**: `{Domain}Controller`, `{Domain}ControllerDocs`, `{Domain}CommandService(Impl)`, `{Domain}QueryService(Impl)`, `{Domain}Repository`, `{Domain}QueryRepository(Impl)`. 메서드 동사: `create`/`update`/`delete`/`get`(단건)/`find`(조건)/`search`/`exists`.
- **SQL**: 테이블·컬럼은 `snake_case`. 제약/인덱스 이름은 명시 — `pk_{table}`, `fk_{from}_{to}`, `idx_{table}_{column}`, `uk_{table}_{column}`.
- **테스트**: 메서드명은 한글 문장형(`핀_생성에_성공한다`), Given-When-Then 구조.
- Swagger 애노테이션은 Controller가 아니라 `controller/docs`의 인터페이스에 작성합니다.

## Git 컨벤션

- 브랜치: `<type>/#<issue-number>-<kebab-설명>` (예: `feat/#14-signup-api`). `develop`에서 분기하고 `develop`으로 PR. `main`/`develop`에 직접 push 금지.
- 커밋: `<type>: <subject>` (subject 한/영 50자 이내, 마침표 없음). type: `feat` `fix` `docs` `refactor` `test` `chore` `rename` `remove`.
- PR 제목: `[Type] 변경 내용` (첫 글자 대문자), 본문에 변경 내용·테스트 결과 작성, `Closes #14`로 이슈 연결. 작업 전 이슈 먼저 생성.
- 이슈와 PR 작성 전 `.github/ISSUE_TEMPLATE/`의 해당 이슈 템플릿과 `.github/PULL_REQUEST_TEMPLATE.md`를 확인하고, 기존 섹션·체크리스트·필수 항목을 유지한 채 실제 작업 내용으로 작성합니다.

자세한 규칙은 `docs/`(ARCHITECTURE.md, CODE_STYLE.md, DATABASE.md, CONVENTION.md)를 참고합니다.
