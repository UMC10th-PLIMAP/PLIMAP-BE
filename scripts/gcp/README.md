# GCP 배포 스크립트

이 디렉터리는 PLIMAP 백엔드의 GCP dev·prod 배포 자동화 파일을 관리합니다. 실제 Cloud SQL, GCS, Redis, VPC, Traefik, DNS/TLS 리소스 생성은 배포 스크립트 실행 전에 별도 운영 절차로 완료해야 합니다.

## 파일 구성

- `deploy-dev.ps1`: Cloud Run dev 서비스를 배포하고 health, Swagger UI, OpenAPI 응답을 검증합니다.
- `deploy-prod.ps1`: Prod revision을 공개 traffic tag 없이 0%로 기동하고 Ready·image digest를 검증한 뒤 트래픽을 전환하며, 최종 검증 실패 시 실제 트래픽 상태를 기준으로 직전 revision을 복구합니다.
- `SECRETS.md`: 환경변수, GitHub Environment Variable, Secret Manager 매핑과 값 교체 방법을 설명합니다.

## 공통 준비

- Google Cloud CLI를 설치하고 `plimap` 프로젝트에 접근 가능한 계정으로 인증합니다.
- 배포할 컨테이너 이미지가 Artifact Registry에 존재해야 합니다.
- 환경별 Secret Manager 항목에 활성 버전이 있어야 합니다.
- Cloud Run runtime service account와 필요한 IAM 권한을 먼저 구성해야 합니다.

환경 설정과 Secret 준비 방법은 [SECRETS.md](SECRETS.md), 전체 인프라 구성은 [Deployment Guide](../../docs/DEPLOYMENT.md)를 참고합니다.

## Dev 실행

저장소 루트에서 다음 명령을 실행합니다.

```powershell
.\scripts\gcp\deploy-dev.ps1
```

GitHub Actions의 `Deploy Dev` 워크플로도 동일한 스크립트를 사용합니다.

## Prod 실행

Prod는 GitHub Actions의 `Deploy Prod` 워크플로 사용을 원칙으로 합니다. `main` push의 `PLIMAP CI`가 성공하면 `prepare` Job이 배포 commit만 확정하고, `production` Environment의 필수 승인자가 **Approve and deploy**를 선택한 뒤에만 GCP 인증과 운영 설정 접근이 시작됩니다.

권한이 있는 운영자가 장애 대응이나 사전 검증을 위해 로컬에서 실행할 때는 immutable image digest, 40자리 commit SHA와 실제 인프라 이름을 명시합니다.

```powershell
.\scripts\gcp\deploy-prod.ps1 `
  -Image "asia-northeast3-docker.pkg.dev/plimap/plimap-docker/api@sha256:<digest>" `
  -DeployCommit "<40-character-commit-sha>" `
  -ProfileImageBucket "<prod-gcs-bucket>" `
  -VpcNetwork "<prod-vpc-network>" `
  -VpcSubnet "<prod-cloud-run-subnet>"
```

스크립트는 다음 순서로 동작합니다.

1. 입력값 형식, 승인된 Artifact Registry repository의 commit SHA image와 immutable digest 일치를 확인합니다.
2. Runtime service account, VPC/subnet, GCS bucket을 확인하고 각 Prod Secret의 `latest`가 가리키는 `ENABLED` 숫자 버전을 확정합니다.
3. 기존 서비스가 있다면 단일 revision이 100% 트래픽을 처리하는지 확인하고, 새 revision을 공개 tag 없이 `--no-traffic`과 deploy health check로 기동합니다.
4. 후보 revision이 Ready이고 실제 resolved image digest가 승인된 digest와 일치하는지 확인합니다.
5. 후보 revision으로 트래픽을 100% 전환하고 실제 트래픽 상태가 단일 100%로 수렴할 때까지 확인합니다.
6. Cloud Run 기본 URL에서 리다이렉션 없이 health JSON의 `status=UP`, 상세 정보 미노출, Swagger/OpenAPI `404`, 민감 Actuator 경로 차단을 검증합니다.
7. 실패 시 실제 트래픽 상태를 다시 조회하고 직전 revision으로 100% 복구한 뒤 트래픽과 health를 재검증합니다.

후보 revision에는 외부에서 호출할 수 있는 traffic tag URL을 만들지 않습니다. 기존 트래픽 revision이 없는 최초 배포는 자동 복구 대상이 없으며, 전환 후 검증 실패 시 `rollback=unavailable-first-deployment`로 기록됩니다. Deploy health check가 후보 컨테이너를 시작하므로 Flyway는 트래픽 전환 전에도 운영 DB에 Migration을 적용할 수 있고, 애플리케이션 rollback은 적용된 Migration을 되돌리지 않습니다.
