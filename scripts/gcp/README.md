# GCP dev 배포 스크립트

## 선행 조건

- GCP project: `plimap`
- Region: `asia-northeast3`
- Artifact Registry: `plimap-docker`
- Runtime service account: `plimap-api-dev@plimap.iam.gserviceaccount.com`
- 아래 Secret Manager 항목에 활성 버전이 있어야 합니다.

| 환경변수 | Secret Manager ID |
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

비밀값은 명령행 인자, 저장소 파일 또는 채팅에 입력하지 않습니다. GCP Console의 Secret Manager에서 각 Secret에 새 버전으로 추가합니다. 구체적인 입력과 교체 절차는 [SECRETS.md](SECRETS.md)를 참고합니다.

`DB_URL`은 Supabase Transaction Pooler를 사용해 다음 형식으로 저장합니다. 사용자명과 비밀번호는 별도 Secret으로 관리합니다.

```text
jdbc:postgresql://{host}:6543/postgres?sslmode=require
```

`REDIS_URL`은 Redis Cloud의 TLS 연결 정보를 사용합니다. 비밀번호에 예약 문자가 있으면 URL 인코딩합니다.

```text
rediss://default:{url-encoded-password}@{host}:{port}
```

## 실행

기본적으로 로컬 프론트엔드 `http://localhost:3000`을 허용합니다.

```powershell
.\scripts\gcp\deploy-dev.ps1
```

dev 프론트엔드가 별도 URL로 배포된 경우 다음처럼 덮어씁니다.

```powershell
.\scripts\gcp\deploy-dev.ps1 `
  -FrontendOrigin "https://dev.example.com" `
  -FrontendRedirectUri "https://dev.example.com/home"
```

스크립트는 다음을 수행합니다.

1. 모든 Secret에 활성 버전이 있는지 확인합니다.
2. `plimap-api-dev` Cloud Run 서비스를 `min 0`, `max 2`로 배포합니다.
3. 최초 배포 후 확정된 Cloud Run URL을 OAuth callback 환경변수에 반영합니다.
4. liveness, readiness, Swagger UI, OpenAPI 응답이 모두 `200`인지 확인합니다.
