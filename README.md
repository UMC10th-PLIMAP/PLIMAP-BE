# PLIMAP-BE

음악과 장소를 연결해 지도 위에 기록하고 공유하는 **PLIMAP**의 백엔드 API입니다.

## 팀원 및 역할

| 팀원 | 담당 영역 |
| --- | --- |
| 주보경 | 인프라 및 클라우드 |
| 이예림 | Member·Auth 도메인, 회원 및 소셜 로그인 |
| 김민주 | Place 도메인 |
| 이서윤 | Track 도메인 |
| 김예원 | Pin 관련 API 설계 |

## 기술 스택

- Java 21, Spring Boot 4.1
- Spring MVC, Spring Data JPA, Hibernate Spatial
- PostgreSQL 18.4
- Gradle, Lombok

## 도메인 구조

도메인 중심의 패키지 구조와 계층형 아키텍처를 사용합니다.

```text
com.example.plimap
├── domain
│   ├── auth       # 인증
│   ├── member     # 회원
│   ├── place      # 장소 및 위치
│   ├── track      # 음악
│   └── pin        # 지도에 남기는 음악·장소 기록
└── global         # 공통 설정, 응답, 예외 및 외부 연동
```

각 도메인은 `controller`, `service`, `repository`, `entity`, `dto` 등으로 구성하며,
서비스는 상태 변경을 담당하는 `command`와 조회를 담당하는 `query`로 분리합니다.

## 로컬 DB 연결

로컬 개발에서는 각 팀원이 PC에 설치한 PostgreSQL/PostGIS를 사용합니다. pgAdmin Desktop에서 생성한 데이터베이스의
접속 정보를 `.env`에 작성하면 Spring Boot가 같은 데이터베이스에 연결됩니다.

### 사전 준비

- PostgreSQL 18 및 PostGIS
- pgAdmin Desktop
- Java 21

### 1. 접속 정보 확인

pgAdmin에서 등록한 PostgreSQL 서버의 `Properties > Connection`에서 host와 port를 확인합니다.
데이터베이스 이름과 로그인에 사용하는 PostgreSQL 사용자(role)의 이름 및 비밀번호도 확인합니다.

### 2. 환경변수 작성

저장소 루트의 `.env.example`을 `.env`로 복사한 뒤 `{}`를 자신의 로컬 설정값으로 교체합니다.

```powershell
Copy-Item .env.example .env
```

```bash
cp .env.example .env
```

```env
DB_URL=jdbc:postgresql://localhost:5432/plimap_local_db
DB_USERNAME=PostgreSQL 사용자명
DB_PASSWORD=PostgreSQL 사용자 비밀번호
```

`.env`에는 개인 로컬 설정이 들어가며 Git에 포함되지 않습니다. 데이터베이스가 기본 포트가 아닌 다른 포트를 사용하면
`DB_URL`의 `5432`를 실제 포트로 변경합니다.

### 3. PostGIS 확인

pgAdmin의 Query Tool에서 다음 쿼리를 실행합니다.

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
SELECT PostGIS_Version();
```

버전 정보가 반환되면 PostGIS를 사용할 수 있습니다.

### 4. Spring Boot 실행

IntelliJ 실행 구성에서 Active profiles를 `local`로 지정하거나 다음 명령을 사용합니다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

실행 로그에 `HikariPool-1 - Start completed`가 표시되면 로컬 PostgreSQL 연결에 성공한 것입니다.

## GitHub 협업 전략 및 컨벤션

### 브랜치 전략

- `main`: 배포 가능한 안정 버전
- `develop`: 기능 통합 및 개발 기준 브랜치
- 작업 브랜치: `develop`에서 생성하고 작업 완료 후 `develop`으로 PR을 요청합니다.
- `main`과 `develop`에는 직접 push하지 않습니다.

### 협업 흐름

1. 작업 시작 전 GitHub Issue를 생성하고 담당자와 라벨을 지정합니다.
2. 하나의 Issue에는 하나의 작업 목적만 포함합니다.
3. Issue 번호가 포함된 작업 브랜치를 생성합니다.
4. 작업 완료 후 `develop` 브랜치로 Pull Request를 생성합니다.
5. PR에 변경 내용과 테스트 결과를 작성하고 `Closes #이슈번호`로 Issue를 연결합니다.
6. 최소 1명의 승인을 받은 뒤 병합하고 작업 브랜치를 삭제합니다.

### 네이밍 컨벤션

- Issue: `[Type] 작업 내용` (예: `[Feature] 회원가입 API 구현`)
- Branch: `<type>/#<issue-number>-<description>` (예: `feat/#14-signup-api`)
- Commit: `<type>: <subject>` (예: `feat: 회원가입 API 구현`)
- PR: `[Type] 변경 내용` (예: `[Feature] 회원가입 API 구현`)
- Issue·PR Type: `Feature`, `Bug`, `Task`, `Document`, `Refactor`
- Branch·Commit type: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`

자세한 내용은 [아키텍처](docs/ARCHITECTURE.md), [협업 컨벤션](docs/CONVENTION.md), [코드 스타일](docs/CODE_STYLE.md)을 참고합니다.
