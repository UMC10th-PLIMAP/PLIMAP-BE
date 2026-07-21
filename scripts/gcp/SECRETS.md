# dev Secret 관리

dev 환경의 비밀값은 저장소나 채팅이 아니라 GCP 프로젝트 `plimap`의 Secret Manager에서 관리합니다.
값 자체는 문서, 명령행 인자, PowerShell history에 남기지 않습니다.

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

## 최초 입력 또는 값 교체

1. GCP Console에서 프로젝트 `plimap`을 선택하고 **Secret Manager**로 이동합니다.
2. 최초 입력이라면 위 ID로 Secret을 만들고, 이미 존재한다면 해당 Secret을 엽니다.
3. **새 버전 추가**를 선택해 새 값을 입력하고 활성화합니다. 기존 활성 버전은 삭제하지 않습니다.
4. `develop`에 빈 커밋을 만들 필요는 없습니다. 다음 dev 배포를 실행하거나, 필요하면 GitHub Actions의 `Deploy Dev`를 수동 실행합니다.

Cloud Run은 새 revision이 시작될 때 `latest` Secret 버전을 주입받으므로, 값을 바꾼 뒤에는 재배포가 필요합니다.

## 연결 정보 형식

### Supabase DB

현재 dev는 Supabase **Transaction Pooler**를 사용합니다. `DB_URL`에는 비밀번호를 넣지 않고, `DB_USERNAME`과 `DB_PASSWORD`를 별도 Secret으로 저장합니다.

```text
DB_URL=jdbc:postgresql://{host}:6543/postgres?sslmode=require
```

### Redis Cloud

AWS Seoul Redis Cloud TLS endpoint를 사용합니다. 비밀번호에 `@`, `:`, `/`, `#`, `%` 같은 예약 문자가 있다면 URL 인코딩합니다.

```text
rediss://default:{url-encoded-password}@{host}:{port}
```

### OAuth

Kakao와 Google에는 dev 전용 OAuth client를 사용합니다. callback URI는 현재 dev API URL을 기준으로 등록합니다.

```text
{dev-api-url}/oauth/callback/kakao
{dev-api-url}/oauth/callback/google
```
