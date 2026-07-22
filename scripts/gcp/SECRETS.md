# dev 환경변수 및 Secret 관리

이 문서는 `deploy-dev.ps1`과 `Deploy Dev` 워크플로가 Cloud Run dev 서비스에 전달하는 환경 설정을 관리합니다.

비밀값은 저장소, 문서, 채팅, 명령행 인자 또는 PowerShell history에 남기지 않고 GCP 프로젝트 `plimap`의 Secret Manager에서 관리합니다.

## 일반 환경변수

다음 값은 비밀정보가 아니며 `deploy-dev.ps1`이 배포 시 생성합니다.

| 애플리케이션 환경변수 | 생성 기준 | dev 기본값 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 스크립트 고정값 | `dev` |
| `CORS_ALLOWED_ORIGINS` | `PublicBaseUrl` | `https://dev.plimap.kr` |
| `OAUTH_REDIRECT_URI` | `FrontendRedirectUri` | `https://dev.plimap.kr/home` |
| `KAKAO_REDIRECT_URI` | `PublicBaseUrl` + callback 경로 | `https://dev.plimap.kr/oauth/callback/kakao` |
| `GOOGLE_REDIRECT_URI` | `PublicBaseUrl` + callback 경로 | `https://dev.plimap.kr/oauth/callback/google` |

GitHub Actions에서는 다음 Repository Variable로 공개 주소를 덮어쓸 수 있습니다. 변수가 없으면 스크립트의 dev 기본값을 사용합니다.

| Repository Variable | 스크립트 인자 | dev 기본값 |
| --- | --- | --- |
| `DEV_PUBLIC_BASE_URL` | `PublicBaseUrl` | `https://dev.plimap.kr` |
| `DEV_FRONTEND_REDIRECT_URI` | `FrontendRedirectUri` | `https://dev.plimap.kr/home` |

`PublicBaseUrl`은 경로, query, fragment, credentials, custom port가 없는 HTTPS Origin이어야 합니다. `FrontendRedirectUri`는 HTTPS URL이어야 합니다.

## Secret Manager 매핑

다음 값은 `deploy-dev.ps1`이 Secret Manager의 `latest` 활성 버전에서 주입합니다.

| 애플리케이션 환경변수 | Secret Manager ID |
| --- | --- |
| `DB_URL` | `plimap-dev-db-url` |
| `DB_USERNAME` | `plimap-dev-db-username` |
| `DB_PASSWORD` | `plimap-dev-db-password` |
| `REDIS_URL` | `plimap-dev-redis-url` |
| `JWT_SECRET` | `plimap-dev-jwt-secret` |
| `KAKAO_REST_API_KEY` | `plimap-dev-kakao-rest-api-key` |
| `KAKAO_REST_API_SECRET` | `plimap-dev-kakao-rest-api-secret` |
| `GOOGLE_CLIENT_ID` | `plimap-dev-google-client-id` |
| `GOOGLE_CLIENT_SECRET` | `plimap-dev-google-client-secret` |
| `YOUTUBE_API_KEY` | `plimap-dev-youtube-api-key` |

## 최초 입력 또는 값 교체

1. GCP Console에서 프로젝트 `plimap`을 선택하고 **Secret Manager**로 이동합니다.
2. 최초 입력이라면 위 ID로 Secret을 만들고, 이미 존재한다면 해당 Secret을 엽니다.
3. **새 버전 추가**를 선택해 새 값을 입력하고 활성화합니다. 기존 활성 버전은 즉시 삭제하지 않습니다.
4. 다음 dev 배포를 실행하거나 GitHub Actions의 `Deploy Dev`를 수동 실행합니다.

Cloud Run은 새 revision이 시작될 때 `latest` Secret 버전을 주입받으므로 값을 바꾼 뒤에는 재배포가 필요합니다. Secret 교체만을 위해 `develop`에 빈 커밋을 만들 필요는 없습니다.

## 연결 정보 형식

### Supabase DB

dev는 Supabase **Transaction Pooler**를 사용합니다. `DB_URL`에는 비밀번호를 넣지 않고 `DB_USERNAME`과 `DB_PASSWORD`를 별도 Secret으로 저장합니다.

```text
DB_URL=jdbc:postgresql://{host}:6543/postgres?sslmode=require
```

### Redis Cloud

Redis Cloud TLS endpoint를 사용합니다. 비밀번호에 `@`, `:`, `/`, `#`, `%` 같은 예약 문자가 있다면 URL 인코딩합니다.

```text
rediss://default:{url-encoded-password}@{host}:{port}
```

### OAuth

Kakao와 Google에는 dev 전용 OAuth client를 사용합니다. 각 Provider Console의 callback URI는 일반 환경변수 표의 `KAKAO_REDIRECT_URI`, `GOOGLE_REDIRECT_URI`와 일치해야 합니다. Cloud Run 원본 URL은 callback URI로 사용하지 않습니다.
