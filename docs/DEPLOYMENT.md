# Deployment Guide

## 환경 구성

| 환경 | 애플리케이션 | PostgreSQL | Redis | Swagger |
| --- | --- | --- | --- | --- |
| local | 개발자 PC | Docker Compose PostGIS | Docker Compose Redis | 활성화 |
| dev | Cloud Run (`asia-northeast3`) | Supabase Postgres/PostGIS | Redis Cloud (`ap-northeast-2`) | 활성화 |
| prod | Cloud Run (`asia-northeast3`) | Cloud SQL for PostgreSQL | 추후 결정 | 비활성화 |

dev와 prod는 동일한 컨테이너 이미지를 사용하고 `SPRING_PROFILES_ACTIVE`로 실행 환경을 구분합니다.

## 컨테이너 빌드

```bash
docker build -t plimap-api:dev .
```

Cloud Run은 컨테이너에 `PORT` 환경변수를 주입합니다. 애플리케이션은 이 값을 사용하며 기본 포트는 `8080`입니다.

## dev 필수 설정

아래 값은 컨테이너 이미지나 저장소에 포함하지 않습니다. Cloud Run 일반 환경변수 또는 Secret Manager로 주입합니다.

| 변수 | 저장 방식 | 설명 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 환경변수 | `dev` |
| `DB_URL` | Secret Manager | Supabase JDBC URL |
| `DB_USERNAME` | Secret Manager | Supabase DB 사용자 |
| `DB_PASSWORD` | Secret Manager | Supabase DB 비밀번호 |
| `REDIS_URL` | Secret Manager | TLS를 사용하는 Redis 접속 URL (`rediss://...`) |
| `JWT_SECRET` | Secret Manager | JWT 서명 키 |
| `KAKAO_REST_API_KEY` | Secret Manager | 카카오 OAuth Client ID |
| `KAKAO_REST_API_SECRET` | Secret Manager | 카카오 OAuth Client Secret |
| `GOOGLE_CLIENT_ID` | Secret Manager | Google OAuth Client ID |
| `GOOGLE_CLIENT_SECRET` | Secret Manager | Google OAuth Client Secret |
| `YOUTUBE_API_KEY` | Secret Manager | YouTube Data API 키 |
| `KAKAO_REDIRECT_URI` | 환경변수 | dev API의 카카오 콜백 URL |
| `GOOGLE_REDIRECT_URI` | 환경변수 | dev API의 Google 콜백 URL |
| `OAUTH_REDIRECT_URI` | 환경변수 | 로그인 완료 후 dev 프론트엔드 URL |
| `CORS_ALLOWED_ORIGINS` | 환경변수 | 쉼표로 구분한 dev 프론트엔드 Origin |

Supabase 연결은 Transaction Pooler 주소와 포트 `6543`을 사용합니다. JDBC URL에는 실제 자격 증명을 넣지 않고 `DB_USERNAME`, `DB_PASSWORD`를 별도 Secret으로 주입합니다. Transaction Pooler의 prepared statement 제약은 dev 프로필의 `prepareThreshold: 0`으로 처리합니다.

## 헬스체크

- 기본 상태: `/actuator/health`
- 시작 및 생존 확인: `/actuator/health/liveness`
- 트래픽 수신 준비 확인: `/actuator/health/readiness`

상세 컴포넌트 정보는 외부에 노출하지 않습니다.

## 초기 Cloud Run 정책

- Region: `asia-northeast3`
- Minimum instances: `0`
- Maximum instances: `2`
- Ingress: all
- Authentication: dev Swagger와 프론트엔드 접근을 위해 public
- Container port: `8080`

dev 배포 자동화는 `develop` 브랜치에 PR이 병합된 뒤 테스트, 이미지 빌드, Artifact Registry push, Cloud Run 배포 순서로 구성합니다.
