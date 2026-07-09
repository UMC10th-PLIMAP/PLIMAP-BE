# 📍PLIMAP-BE

음악과 장소를 연결해 지도 위에 기록하고 공유하는 **PLIMAP**의 백엔드 API입니다.

## 팀원 및 역할

| 팀원 | 담당 영역                           |
| --- |---------------------------------|
| 주보경 | 🐘 인프라 및 클라우드                   |
| 이예림 | 👥 Member·Auth 도메인, 회원 및 소셜 로그인 |
| 김민주 | 🗺️ Place 도메인                   |
| 이서윤 | 📍 Pin 관련 API 설계                |
| 김예원 | 🎶 Track 도메인                    |

## 기술 스택

- Java 21, Spring Boot 4.1
- Spring MVC, Spring Data JPA, Hibernate Spatial
- PostgreSQL 18.4, PostGIS

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
서비스는 상태 변경을 담당하는 `command`와 조회를 담당하는 `query`로 분리하여 부분적으로 CQRS 아키텍져 전략을 취합니다.

## 로컬 실행 방법

### 사전 준비

- Java 21
- Docker 및 Docker Compose

### 실행 방법

1. 필요하면 환경 변수 예시 파일을 복사해 로컬 값을 변경합니다. 기본값을 그대로 사용하면 생략할 수 있습니다.

   ```bash
   # macOS/Linux
   cp .env.example .env

   # Windows PowerShell
   Copy-Item .env.example .env
   ```

2. Docker Compose로 로컬 PostGIS 데이터베이스를 실행합니다.

   ```bash
   docker compose up -d
   ```

3. `local` 프로필로 Spring Boot 애플리케이션을 실행합니다. 최초 실행 시 Flyway가 PostGIS 확장과 전체 스키마를 생성합니다.

   ```bash
   # macOS/Linux
   ./gradlew bootRun --args='--spring.profiles.active=local'

   # Windows
   .\gradlew.bat bootRun --args="--spring.profiles.active=local"
   ```

   IntelliJ **Active profiles**에 `local`을 지정.

4. 애플리케이션 실행 후 Swagger UI 동작을 확인합니다.

   ```text
   http://localhost:8080/swagger-ui/index.html
   ```

### 종료 방법

```bash
docker compose down
```

데이터까지 초기화하려면 `docker compose down -v`를 실행.

데이터베이스 Migration, QueryDSL 및 테스트 규칙은 [데이터베이스 개발 가이드](docs/DATABASE.md)를 참고합니다.

## GitHub 협업 전략 및 컨벤션

<details>
<summary><strong>협업 흐름</strong></summary>

1. 작업 시작 전 GitHub Issue를 생성하고 담당자와 라벨을 지정합니다.
2. 하나의 Issue에는 하나의 작업 목적만 포함합니다.
3. Issue 번호가 포함된 작업 브랜치를 생성합니다.
4. 작업 완료 후 `develop` 브랜치로 Pull Request를 생성합니다.
5. PR에 변경 내용과 테스트 결과를 작성하고 `Closes #이슈번호`로 Issue를 연결합니다.
6. 최소 1명의 승인을 받은 뒤 병합하고 작업 브랜치를 삭제합니다.

</details>

<details>
<summary><strong>브랜치 전략</strong></summary>

- `main`: 배포 가능한 안정 버전
- `develop`: 기능 통합 및 개발 기준 브랜치
- 작업 브랜치: `develop`에서 생성하고 작업 완료 후 `develop`으로 PR을 요청합니다.
- `main`과 `develop`에는 직접 push하지 않습니다.

</details>

<details>
<summary><strong>네이밍 컨벤션</strong></summary>

- Issue: `[Type] 작업 내용` (예: `[Feature] 회원가입 API 구현`)
- Branch: `<type>/#<issue-number>-<description>` (예: `feat/#14-signup-api`)
- Commit: `<type>: <subject>` (예: `feat: 회원가입 API 구현`)
- PR: `[Type] 변경 내용` (예: `[Feature] 회원가입 API 구현`)

</details>

자세한 내용은 [docs](./docs)를 참고합니다.
