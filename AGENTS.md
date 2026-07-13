# AGENTS.md

이 문서는 PLIMAP 저장소에서 작업하는 AI 에이전트와 개발자를 위한 작업 지침입니다. 저장소 루트와 모든 하위 경로에 적용되며, 하위 디렉터리에 더 구체적인 `AGENTS.md`가 있다면 해당 문서의 규칙을 우선합니다.

## 1. 프로젝트 개요

- PLIMAP은 음악과 장소를 연결해 지도에 기록하고 공유하는 백엔드 API입니다.
- Java 21과 Spring Boot 4.1을 사용합니다.
- 주요 기술은 Spring MVC, Spring Data JPA, Spring Security, OAuth2, JWT, QueryDSL, PostgreSQL/PostGIS, Flyway입니다.
- 패키지 루트는 `com.example.plimap`입니다.
- 도메인은 현재 `auth`, `member`, `place`, `track`을 중심으로 구성되며, 공통 기능은 `global`에 둡니다.

## 2. 먼저 확인할 문서

변경 범위에 따라 아래 문서를 먼저 읽고 기존 규칙을 따릅니다.

- 전체 안내와 로컬 실행: `README.md`
- 아키텍처와 패키지 책임: `docs/ARCHITECTURE.md`
- Java, API, 엔티티, 테스트 스타일: `docs/CODE_STYLE.md`
- Flyway, QueryDSL, Testcontainers: `docs/DATABASE.md`
- Git, 브랜치, 커밋, PR 규칙: `docs/CONVENTION.md`
- 스키마 및 관계 변경: `docs/ERD.md`

문서와 실제 코드가 다르면 임의로 대규모 정리하지 말고, 현재 작업과 관련된 범위에서 차이를 확인한 뒤 코드와 문서를 함께 일관되게 갱신합니다.

## 3. 주요 디렉터리

```text
src/main/java/com/example/plimap/
├── domain/                  # 도메인별 기능 코드
│   └── {domain}/
│       ├── controller/      # HTTP 요청/응답
│       ├── dto/             # 요청/응답 DTO
│       ├── entity/          # JPA 엔티티
│       ├── exception/       # 도메인 오류 코드와 예외
│       ├── repository/      # 데이터 접근
│       └── service/         # command/query 서비스
└── global/                  # 공통 응답, 설정, 보안, 공통 엔티티

src/main/resources/
├── application.yml         # 공통 설정
├── application-prod.yml    # 운영 설정
└── db/migration/            # Flyway Migration

src/test/                   # 단위 및 통합 테스트
docs/                       # 개발 문서
```

생성되는 QueryDSL Q 타입과 빌드 산출물은 `build/` 아래에만 두며 커밋하지 않습니다.

## 4. 작업 원칙

1. 요청 범위와 관련된 기존 구현 및 테스트를 먼저 확인합니다.
2. 변경은 가능한 한 작고 응집력 있게 유지하며, 무관한 리팩터링이나 포맷 변경을 섞지 않습니다.
3. 기존 공개 API, DB 스키마, 보안 정책을 근거 없이 변경하지 않습니다.
4. 사용자가 만든 변경 사항을 덮어쓰거나 되돌리지 않습니다.
5. 새 의존성은 기존 의존성으로 해결할 수 없는지 확인한 뒤 최소한으로 추가합니다.
6. 동작이나 개발 절차가 달라지면 관련 `docs/` 문서와 예시 설정도 함께 갱신합니다.
7. 에러를 숨기기 위한 임시 우회, 빈 예외 처리, 과도한 기본값을 추가하지 않습니다.

## 4. Prohibited Commit Message Content
커밋 메시지에는 다음 내용을 절대 포함하지 않습니다.

Generated with Codex
Generated with Claude Code
Co-Authored-By: Codex
Co-Authored-By: Claude
AI가 생성했다는 어떤 표시

## 5. 아키텍처 규칙

- 기본 의존 방향은 `Controller -> Service -> Repository -> Entity`입니다.
- Controller는 요청 바인딩, 검증, Service 호출, 공통 응답 반환만 담당합니다.
- 비즈니스 로직은 Service 또는 의미 있는 Entity 메서드에 둡니다.
- Service는 상태 변경을 담당하는 `command`와 읽기 전용인 `query`로 분리합니다.
- Command 구현체에는 필요에 따라 `@Transactional`, Query 구현체에는 `@Transactional(readOnly = true)`를 적용합니다.
- 다른 도메인의 Repository를 직접 주입하지 말고 해당 도메인의 Service 인터페이스를 사용합니다.
- 도메인 사이에 순환 의존이 생기지 않도록 합니다.
- 단순 조회는 Spring Data JPA, 동적 조건·복잡한 검색·페이징은 QueryDSL을 우선합니다.
- Query Repository는 데이터 접근에만 집중하고 API 응답 DTO나 비즈니스 상태 변경에 의존하지 않습니다.

## 6. API와 DTO 규칙

- 모든 API 응답은 `ApiResponse<T>`와 의미에 맞는 HTTP 상태를 사용합니다.
- 반환 데이터가 없는 성공 응답은 `ApiResponse<Void>`로 표현합니다.
- Entity를 API 요청 또는 응답에 직접 노출하지 않습니다.
- Request DTO와 Response DTO를 분리하고, DTO는 기본적으로 `record`를 사용합니다.
- 단순 Entity-to-DTO 변환은 응답 DTO의 `from()`으로 처리하고, 복합 변환만 별도 Converter에 둡니다.
- Swagger 설명은 Controller 구현에 섞지 않고 `controller/docs`의 인터페이스에 작성합니다.
- 도메인 예외는 `BusinessException`을 상속한 도메인 예외로 변환해 던지며, Controller에서 직접 try-catch 하지 않습니다.
- Bean Validation 메시지는 한글로 작성합니다.

## 7. Entity와 영속성 규칙

- Entity는 필요한 Lombok만 제한적으로 사용합니다.
- `@Getter`와 `@NoArgsConstructor(access = AccessLevel.PROTECTED)`를 기본으로 하며 `@Setter`, `@Data`는 사용하지 않습니다.
- 생성은 private 생성자의 `@Builder`와 의미 있는 정적 팩터리 메서드를 사용합니다. 클래스 레벨 `@Builder`는 피합니다.
- 상태 변경은 Setter 대신 `update...`, `delete`, `restore`처럼 의도가 드러나는 메서드로 표현합니다.
- 연관관계는 기본적으로 `FetchType.LAZY`를 사용하고 불필요한 양방향 매핑을 만들지 않습니다.
- Enum은 반드시 `EnumType.STRING`으로 저장합니다.
- 모든 Entity는 원칙적으로 `BaseEntity`를 상속하고 시간 값은 `Instant`를 사용합니다.
- Soft Delete 대상에는 `repository.delete()`나 `deleteById()`를 사용하지 않습니다. `delete()`/`restore()`를 호출하고 일반 조회에는 `deletedAt IS NULL` 조건을 적용합니다.
- 현재 Soft Delete 대상은 `Member`, `Place`, `PlaceTrack`, `Pin`입니다. 대상 변경 시 문서와 Migration을 함께 검토합니다.

## 8. 데이터베이스와 Migration

- 스키마 변경은 Hibernate 자동 생성이 아니라 Flyway Migration으로만 수행합니다.
- 후속 파일명은 `VyyyyMMddHHmm__snake_case_description.sql` 형식을 사용합니다.
- 하나의 Migration에는 하나의 논리적인 스키마 변경만 포함합니다.
- 이미 공유 브랜치에 병합되거나 적용된 Migration은 수정·삭제하지 말고, 더 높은 버전의 새 파일을 추가합니다.
- 테이블과 컬럼은 `snake_case`를 사용하고 PK, FK, Index, Unique Index 이름을 명시합니다.
- 파괴적 DDL과 대량 데이터 변경은 데이터 손실과 잠금 영향을 검토하고 확장-전환-정리 단계로 나눕니다.
- 공유 DB에서 `flyway clean`을 실행하지 않습니다.
- Migration과 관련 Entity 변경 및 테스트는 같은 작업에서 함께 검증합니다.

## 9. 보안과 설정

- `.env`와 실제 인증 정보, JWT secret, OAuth client secret, DB 비밀번호를 읽어서 출력하거나 커밋하지 않습니다.
- 예시는 `.env.example`에 가짜 값과 변수 설명만 추가합니다.
- 로그, 테스트 픽스처, 문서에 토큰이나 개인정보를 남기지 않습니다.
- 인증·인가, CORS, 쿠키, CSRF 변경은 기존 보안 테스트와 운영 환경 영향을 함께 검토합니다.
- 테스트에는 실제 외부 서비스 키 대신 `application-test.yml`의 테스트 전용 값을 사용합니다.

## 10. 빌드와 실행 명령

Windows PowerShell 기준:

```powershell
# 전체 테스트
.\gradlew.bat test

# 정리 후 전체 빌드 및 검증
.\gradlew.bat clean build

# 특정 테스트 클래스
.\gradlew.bat test --tests "com.example.plimap.SomeTest"

# 로컬 PostGIS 실행
docker compose up -d

# 로컬 프로필로 애플리케이션 실행
.\gradlew.bat bootRun --args="--spring.profiles.active=local"

# 컨테이너 종료(볼륨 유지)
docker compose down
```

macOS/Linux에서는 `./gradlew`를 사용합니다. DB 통합 테스트는 PostGIS Testcontainers를 사용하므로 Docker가 실행 중이어야 합니다. `docker compose down -v`는 로컬 데이터를 삭제하므로 사용자가 명시적으로 요청하거나 초기화가 반드시 필요한 경우에만 실행합니다.

## 11. 테스트 기준

- 버그 수정은 가능하면 실패를 재현하는 테스트를 먼저 추가합니다.
- 테스트 메서드는 `핀_생성에_성공한다`와 같은 한글 문장형으로 작성합니다.
- 테스트 본문은 `given`, `when`, `then` 구조를 따릅니다.
- 비즈니스 로직 변경에는 Service 단위 테스트를 추가합니다.
- QueryDSL 또는 복잡한 Repository 변경에는 PostGIS Testcontainers 기반 통합 테스트를 작성합니다.
- Controller 계약 변경에는 WebMvc 테스트를 추가합니다.
- 시간, 정렬, 페이징, Soft Delete, 권한 경계 조건을 명시적으로 검증합니다.
- 작업 완료 전 최소한 관련 테스트를 실행하고, 가능하면 `clean build`까지 수행합니다.
- Docker나 외부 환경 문제로 검증하지 못한 항목은 완료 보고에 명확히 남깁니다.

## 12. 네이밍과 Git 규칙

- 클래스와 메서드 이름은 `docs/CODE_STYLE.md`의 역할별 접미사와 동사 규칙을 따릅니다.
- 커밋 형식은 `<type>: <subject>`이며 subject는 50자 이내, 마침표 없이 작성합니다.
- 주요 type은 `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `rename`, `remove`입니다.
- 브랜치는 `develop`에서 만들고 `<type>/#<issue-number>-<kebab-case-description>` 형식을 사용합니다.
- PR은 `develop`을 대상으로 하며 변경 내용, 테스트 결과, `Closes #이슈번호`를 포함합니다.
- 이슈와 PR을 작성하기 전에 `.github/ISSUE_TEMPLATE/`의 해당 이슈 템플릿과 `.github/PULL_REQUEST_TEMPLATE.md`를 확인하고, 기존 섹션·체크리스트·필수 항목을 유지한 채 실제 작업 내용으로 작성합니다.
- 에이전트는 사용자의 명시적 요청 없이 브랜치 생성, 커밋, push, PR 생성 또는 기존 이력 변경을 수행하지 않습니다.

## 13. 완료 체크리스트

작업을 마치기 전에 다음을 확인합니다.

- 요청한 동작과 완료 조건을 충족했는가?
- 계층 책임과 command/query 분리를 지켰는가?
- API 계약이나 DB 변경에 필요한 문서와 Migration을 반영했는가?
- 기존 기능을 깨뜨리지 않는 테스트를 추가하고 실행했는가?
- 생성된 Q 타입, 빌드 산출물, 비밀 정보가 변경 목록에 포함되지 않았는가?
- 무관한 파일이나 사용자 변경을 건드리지 않았는가?

완료 보고에는 변경한 내용, 실행한 검증, 남아 있는 제약이나 미실행 검증을 간결하게 적습니다.
