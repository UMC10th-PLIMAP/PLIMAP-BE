# GCP dev 배포 스크립트

이 디렉터리는 PLIMAP 백엔드의 GCP dev 배포 자동화 파일을 관리합니다.

## 파일 구성

- `deploy-dev.ps1`: Cloud Run dev 서비스를 배포하고 헬스체크, Swagger UI, OpenAPI 응답을 검증합니다.
- `SECRETS.md`: 배포에 사용하는 환경변수, Secret Manager 매핑, 값 입력 및 교체 방법을 설명합니다.

## 실행 전 준비

- Google Cloud CLI를 설치하고 `plimap` 프로젝트에 접근할 수 있는 계정으로 인증합니다.
- 배포에 필요한 Secret Manager 항목에 활성 버전이 있는지 확인합니다.
- 배포할 컨테이너 이미지가 Artifact Registry에 존재하는지 확인합니다.

환경 설정과 Secret 준비 방법은 [SECRETS.md](SECRETS.md)를 참고합니다.

## 실행

저장소 루트에서 다음 명령을 실행합니다.

```powershell
.\scripts\gcp\deploy-dev.ps1
```

GitHub Actions의 `Deploy Dev` 워크플로도 동일한 스크립트를 사용합니다.

도메인, DNS, Traefik 라우팅을 포함한 전체 배포 구성은 [Deployment Guide](../../docs/DEPLOYMENT.md)를 참고합니다.
