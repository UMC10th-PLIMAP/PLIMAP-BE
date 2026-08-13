# Prod 외부 인프라 운영 및 재구축 절차

이 문서는 현재 Prod 외부 인프라 기준과 신규 프로젝트·재해 복구 재구축 절차를 함께 관리합니다. 운영 리소스는 이미 활성화되어 있으므로 아래 적용 명령을 일상 배포나 설정 확인 목적으로 실행하지 않습니다. 재구축 범위, 비용, 대상 프로젝트와 동명 리소스 충돌을 별도로 승인받은 경우에만 사용합니다.

## 재구축 적용 게이트

1. 재구축 대상과 복구 시점, 예상 비용을 승인받고 기존 운영 리소스와 분리된 대상인지 확인합니다.
2. PostgreSQL 17에서 PostGIS 3.5.2 설치 가능 여부, 전체 Flyway Migration, 런타임 CRUD와 권한 음성 테스트를 별도 환경에서 검증합니다.
3. 아래 기준과 최신 월 비용을 다시 제시하고 명시적 적용 승인을 받습니다. 동명 리소스가 있으면 수정하거나 재생성하지 않고 중단합니다.
4. Secret 값, DB 비밀번호, OAuth 클라이언트 Secret과 Redis 자격 증명은 명령행 인자, 저장소, 이슈 또는 로그에 넣지 않습니다.

2026-08-06의 임시 PG17 검증은 최초 구축 의사결정의 역사적 근거입니다. 재구축 시에는 현재 Cloud SQL 제공 버전과 권한 동작을 다시 검증하며, 임시 리소스의 범위·시간·삭제도 별도로 승인합니다.

## 현재 리소스 기준

| 리소스 | 현재 기준 |
| --- | --- |
| 프로젝트 / 번호 | `plimap` / `902362979890` |
| 리전 | `asia-northeast3` |
| VPC | `plimap-prod-vpc`, 커스텀 모드 |
| Cloud Run 서브넷 | `plimap-prod-run`, `10.20.0.0/24`, Private Google Access 활성화 |
| Private Service Access 대역 | `google-managed-services-plimap-prod-vpc`, `10.21.0.0/24` |
| Cloud SQL | `plimap-prod-postgres`, PostgreSQL 17, Enterprise, 적용 tier `db-custom-1-3840` |
| DB | `plimap_prod` |
| Cloud Run | `plimap-api-prod`, 애플리케이션 1 vCPU/1 GiB, 서비스 min 0/max 3, sample Bootstrap은 재구축 시에만 사용 |
| 프론트 Cloud Run | `plimap-web-prod`, 1 vCPU/512 MiB, min 0/max 2, 요청 기반 과금 |
| 런타임 서비스 계정 | `plimap-api-prod@plimap.iam.gserviceaccount.com` |
| 배포 서비스 계정 | `plimap-github-prod-deployer@plimap.iam.gserviceaccount.com` |
| 프론트 서비스 계정 | `plimap-web-prod@plimap.iam.gserviceaccount.com`, `plimap-web-prod-deployer@plimap.iam.gserviceaccount.com` |
| WIF | pool `github-actions`, 공급자 `plimap-be-prod`, `plimap-web-prod` |
| GCS | `plimap-prod-profile-images`, Standard, uniform bucket-level access |
| Load Balancer | Global External Application Load Balancer, Premium Tier, IPv4 전용, CDN·Cloud Armor 비활성 |
| 공개 IPv4 | `plimap-prod-lb-ip` / `8.233.213.169` |
| TLS | `plimap.kr`용 Google-managed certificate `plimap-prod-cert` |

2026-08-13 확인 기준으로 VPC, 서브넷, Private Service Access 대역, 서비스 계정, WIF 공급자, GCS 버킷, 영구 private IP Cloud SQL, API·프론트 Cloud Run 애플리케이션과 Global External Application Load Balancer가 운영 중입니다. `plimap.kr` DNS와 Managed TLS가 활성화되어 공개 경로 점검를 수행합니다. Secret 값·활성 버전 수와 현재 리비전 이름은 문서에 고정하지 않고 배포 시 제어 영역에서 확인합니다.

Cloud Run 서브넷은 Direct VPC 최소 크기인 `/26`보다 크게 구성해 리비전 중첩 여유를 확보했습니다. Private Service Access 대역은 별도의 `/24`이며, Cloud SQL은 리전과 DB 유형별로 `/24`를 사용합니다. 두 대역 모두 현재 `asia-northeast3` 기본 VPC 서브넷 `10.178.0.0/20`과 겹치지 않습니다.

### GCE 사용 여부

Prod에는 GCE 인스턴스를 만들거나 개인 서버의 Traefik을 재사용하지 않습니다. 승인된 구성은 Global External Application Load Balancer를 사용해 프론트와 API를 동일한 GCP 프로젝트에서 운영하고 `https://plimap.kr`을 공유합니다. Dev는 개인 서버 Traefik을 독립적으로 계속 사용합니다.

## 영구 Cloud SQL 사양

| 설정 | 값 |
| --- | --- |
| Edition / machine | Enterprise / 1 vCPU, 3840 MiB (`db-custom-1-3840`) |
| 가용성 | 단일 Zone |
| 저장소 | SSD 10 GiB, 자동 증가 활성화, 최대 50 GiB |
| 네트워크 | `plimap-prod-vpc`의 private IP 전용 |
| Connector enforcement | 애플리케이션이 private IP JDBC로 직접 연결하므로 `NOT_REQUIRED` |
| TLS | `ENCRYPTED_ONLY`, JDBC는 `sslmode=require` 사용 |
| 백업 | 매일 15:00 UTC, 백업 8개 보존 |
| PITR | 활성화, transaction log 7일 보존 |
| 유지보수 | 토요일 19:00 UTC, 일요일 04:00 KST |
| 보호 | 삭제 방지 활성화 |

현재 안정 버전 gcloud CLI에서는 `--cpu=1 --memory=3840MiB`를 사용합니다. 생성되는 Enterprise 커스텀 tier는 `db-custom-1-3840`입니다. PostgreSQL에는 MySQL 전용 옵션인 `--enable-auto-upgrade-minor-version`을 전달하지 않습니다.

## 비용 계획 참고값

이 추정치는 재구축이나 중요한 용량 변경 전에 다시 산정해야 합니다. 청구 금액이나 강제 상한이 아닙니다.

| 구성요소 | 계획 추정치 |
| --- | ---: |
| Cloud SQL 서울 단일 Zone 컴퓨팅, 1 vCPU/3.75 GiB, 730시간 | 약 USD 64.11 |
| Cloud SQL SSD 10 GiB | 약 USD 2.21 |
| Secret Manager, 각 1개 버전을 가진 Secret 12개 | 최대 약 USD 0.72 |
| GCS Standard 서울, 예시 10 GiB | 약 USD 0.22 |
| 선택한 Redis Cloud 요금제 | 약 USD 7.00 |
| Global external LB 전달 규칙 기본 비용, 최초 5개 규칙 | 약 USD 18.25 |
| VPC/Private Service Access/Cloud Run min 0/GCE | 고정 유휴 비용 USD 0 |
| **계획 합계** | **월 약 USD 92~95** |

기본 제공량을 넘는 백업 저장소, Cloud Run 활성 인스턴스 시간, Artifact Registry 저장소, Load Balancer 트래픽 처리, GCS·Redis·Cloud Run 외부 전송과 모니터링 트래픽은 변동 비용입니다. 검토 기준은 월 USD 110이며 프로젝트 예산 알림 목표는 기존 USD 80에서 최소 USD 100으로 올리고 50%, 75%, 90%, 100% 구간을 설정합니다. 예산 알림은 리소스를 자동으로 중지하지 않습니다.

## 신규 프로젝트·재해 복구 적용 순서

### 1. 리소스 충돌 재확인 및 API 활성화

대상 프로젝트, VPC, 서브넷, 할당 대역, Cloud SQL, Cloud Run, GCS, 서비스 계정, WIF 공급자와 Prod Secret ID를 다시 조회합니다. 충돌 리소스가 없고 별도 승인된 재구축인 경우에만 계속하며, 그렇지 않으면 어떤 변경도 하기 전에 중단합니다.

```powershell
gcloud services enable `
  compute.googleapis.com `
  run.googleapis.com `
  sqladmin.googleapis.com `
  servicenetworking.googleapis.com `
  secretmanager.googleapis.com `
  iamcredentials.googleapis.com `
  sts.googleapis.com `
  --project=plimap
```

### 2. 격리된 VPC와 Private Service Access 생성

```powershell
gcloud compute networks create plimap-prod-vpc `
  --project=plimap `
  --subnet-mode=custom `
  --bgp-routing-mode=regional

gcloud compute networks subnets create plimap-prod-run `
  --project=plimap `
  --network=plimap-prod-vpc `
  --region=asia-northeast3 `
  --range=10.20.0.0/24 `
  --enable-private-ip-google-access

gcloud compute addresses create google-managed-services-plimap-prod-vpc `
  --project=plimap `
  --global `
  --purpose=VPC_PEERING `
  --network=plimap-prod-vpc `
  --addresses=10.21.0.0 `
  --prefix-length=24

gcloud services vpc-peerings connect `
  --project=plimap `
  --service=servicenetworking.googleapis.com `
  --network=plimap-prod-vpc `
  --ranges=google-managed-services-plimap-prod-vpc
```

기존 Cloud Run 서비스 에이전트에는 Direct VPC 권한이 포함된 `roles/run.serviceAgent`가 이미 있습니다. 프로젝트 전체 범위의 `roles/compute.networkUser`를 중복으로 부여하지 않습니다. GitHub 배포 계정에는 새 리비전에서도 승인된 서브넷을 유지할 수 있도록 `plimap-prod-run`에 대해서만 `roles/compute.networkUser`를 부여하며, 다른 서브넷 접근 권한은 주지 않습니다.

### 3. 서비스 계정과 Prod WIF 공급자 생성

문서에 지정된 서비스 계정만 생성합니다. 배포 계정에는 프로젝트 전체 범위의 `roles/run.admin`이나 Secret 값 접근 권한을 부여하지 않습니다.

공급자 조건은 다음과 같습니다.

```text
assertion.repository == 'UMC10th-PLIMAP/plimap-be'
&& assertion.ref == 'refs/heads/main'
&& assertion.environment == 'production'
&& assertion.workflow_ref ==
   'UMC10th-PLIMAP/plimap-be/.github/workflows/deploy-prod.yml@refs/heads/main'
```

`google.subject`, `attribute.repository`, `attribute.ref`, `attribute.environment`, `attribute.workflow_ref`를 매핑합니다. Prod 배포 계정의 `roles/iam.workloadIdentityUser`는 `attribute.repository`가 `UMC10th-PLIMAP/plimap-be`인 pool principal set에만 부여합니다. 기존 Dev provider `plimap-be`는 변경하지 않습니다.

### 4. GCS 버킷과 버킷 IAM 생성

```powershell
gcloud storage buckets create gs://plimap-prod-profile-images `
  --project=plimap `
  --location=asia-northeast3 `
  --default-storage-class=STANDARD `
  --uniform-bucket-level-access `
  --soft-delete-duration=0 `
  --no-public-access-prevention
```

객체 버전 관리가 비활성화되어 있는지 확인합니다. 런타임 서비스 계정에는 `roles/storage.objectUser`를 부여합니다. `allUsers`에는 버킷 단위 `roles/storage.legacyObjectReader`를 부여한 뒤 알려진 기존 객체를 익명으로 가져올 수 있고 익명 객체 목록 조회는 실패하는지 확인합니다. 조직의 Public Access Prevention 정책이 IAM binding을 차단하면 중단합니다. 목록 조회까지 허용하는 `roles/storage.objectViewer`로 대체하지 않습니다.

### 5. Secret 리소스와 최소 권한 IAM 생성

[`SECRETS.md`](SECRETS.md)의 Secret ID 12개를 값 버전 없이 생성합니다. 각 Secret에는 다음 권한만 부여합니다.

- 런타임 서비스 계정: `roles/secretmanager.secretAccessor`
- Prod 배포 계정: 버전 메타데이터 조회 전용 `roles/secretmanager.viewer`

버전 생성 후 배포 계정이 `latest` 메타데이터를 조회할 수 있지만 `secrets versions access`는 실행할 수 없는지 확인합니다. Dev Secret 연결은 없어야 합니다.

### 6. 영구 Cloud SQL 생성

승인된 신규 인스턴스 또는 재해 복구 재구축에서만 현재 기본 버전을 기록하고 PostGIS 3.5.2를 설치할 수 있는지 먼저 확인합니다.

```powershell
gcloud sql instances create plimap-prod-postgres `
  --project=plimap `
  --database-version=POSTGRES_17 `
  --edition=ENTERPRISE `
  --cpu=1 `
  --memory=3840MiB `
  --region=asia-northeast3 `
  --availability-type=ZONAL `
  --network=projects/plimap/global/networks/plimap-prod-vpc `
  --no-assign-ip `
  --connector-enforcement=NOT_REQUIRED `
  --ssl-mode=ENCRYPTED_ONLY `
  --storage-type=SSD `
  --storage-size=10 `
  --storage-auto-increase `
  --storage-auto-increase-limit=50 `
  --backup-start-time=15:00 `
  --retained-backups-count=8 `
  --enable-point-in-time-recovery `
  --retained-transaction-log-days=7 `
  --maintenance-window-day=SAT `
  --maintenance-window-hour=19 `
  --deletion-protection `
  --labels=environment=prod,service=plimap

gcloud sql databases create plimap_prod `
  --project=plimap `
  --instance=plimap-prod-postgres `
  --charset=UTF8
```

생성 후 공개 IP가 없고 private network가 승인된 VPC와 일치하는지, 백업·PITR·삭제 방지 설정이 기준과 일치하는지, 실제 DB 버전과 PostGIS 확장 버전이 승인 게이트를 계속 충족하는지 확인합니다.

### 7. DB 역할 초기 구성

기본 관리자 비밀번호와 두 전용 역할의 비밀번호는 명령 이력에 남지 않는 운영자 경로로 설정합니다. [`PROD_DATABASE.md`](PROD_DATABASE.md)에 따라 다음 순서로 실행합니다.

1. Cloud SQL 관리자로 `plimap_prod`에 연결해 `bootstrap-prod-database.sql`을 실행합니다.
2. 재구축한 DB에서 Flyway를 실행하기 전에 `plimap_migrator`로 `configure-prod-database-grants.sql`을 실행합니다.
3. 기존 객체는 각 객체 소유자로 `grant-prod-database-existing-objects.sql`을 별도로 실행합니다.
4. 자격 증명이 없는 JDBC URL과 두 역할의 자격 증명을 각각 분리된 Secret 버전에 저장합니다.
5. 최종 Flyway Migration이 `flyway_schema_history`에 대한 `plimap_app`의 모든 접근 권한을 회수했는지 확인합니다.

### 8. IAM 재구축 및 Cloud Run 초기 구성

Prod 배포 계정에는 다음 권한만 부여합니다.

- `plimap-docker` repository의 `roles/artifactregistry.writer`
- Prod 런타임 서비스 계정의 `roles/iam.serviceAccountUser`
- `plimap-prod-run` 서브넷의 `roles/compute.networkUser`
- 각 Prod Secret의 메타데이터 조회 권한

승인된 재구축에서만 먼저 `bootstrap-prod-cloud-run.ps1`을 `-Apply` 없이 실행해 계획을 검토합니다. 대상이 비어 있음을 확인한 뒤에만 인프라 관리자가 `-Apply`를 추가할 수 있습니다. 스크립트는 필요한 샘플 초기 리비전을 만들고 기본 URL을 비활성화하며 ingress를 Cloud Load Balancing으로 제한하고 문서에 정의한 서비스 단위 IAM만 부여합니다. 현재 운영 중인 Prod 서비스에는 실행하지 않습니다.

### 9. GitHub Environment 메타데이터 설정

비밀값이 아닌 다음 `production` 환경변수를 설정합니다.

```text
PROD_PUBLIC_BASE_URL=https://plimap.kr
PROD_FRONTEND_REDIRECT_URI=https://plimap.kr/app/oauth/callback
PROD_CORS_ALLOWED_ORIGINS=https://plimap.kr,https://admin.plimap.kr
PROD_OAUTH_ALLOWED_FRONTEND_ORIGINS=https://plimap.kr,https://admin.plimap.kr
PROD_GCS_BUCKET=plimap-prod-profile-images
PROD_VPC_NETWORK=plimap-prod-vpc
PROD_VPC_SUBNET=plimap-prod-run
PROD_PUBLIC_SMOKE_ENABLED=true
```

`main`만 배포 브랜치로 유지하고 GCP 인증 전에 GitHub `production` Environment 승인을 요구합니다. 팀이 지원할 수 있는 범위에서 최소 권한과 직무 분리를 적용하며, 편의를 위해 검토자 보호를 약화하지 않습니다.

## 재구축 후 및 정기 검증

- Cloud SQL: private IP 전용, PostGIS·Flyway·CRUD·최소 권한 증빙, 백업과 PITR 상태
- Cloud Run: API·프론트 서비스의 `internal-and-cloud-load-balancing` ingress, 기본 URL 비활성, 승인된 애플리케이션 리비전 하나가 트래픽 100% 처리
- IAM: 배포 계정은 Secret 값에 접근하거나 `plimap-api-dev`를 변경할 수 없고, 런타임은 Prod Secret 값과 Prod bucket에만 접근
- GCS: 알려진 객체의 익명 GET 성공, 익명 목록 조회 실패, 런타임 업로드·삭제 성공
- WIF: 정확한 main/production/deploy-prod 워크플로만 Prod 배포 계정으로 인증 가능
- Artifact Registry: 정리 정책 사전 점검 전에 현재·롤백 이미지 digest를 기록하고 별도 검토 없이 삭제를 활성화하지 않음
- 복구: 별도 비용 승인을 받은 PITR 복구 훈련에서 PostGIS와 Flyway history를 검증한 뒤 복구 인스턴스 삭제
- Load Balancer: HTTP→HTTPS redirect, `/api/**`·`/oauth/**`는 API NEG, 나머지는 프론트 NEG로 전달되며 `plimap.kr` 관리형 인증서가 활성 상태
- 모니터링: Cloud Run 5xx·p95·최대 인스턴스, Cloud SQL CPU·memory·connection·disk, Redis 사용량·연결, 외부 가동 상태과 예산 알림

## 지속 운영 항목

1. Prod Secret은 Dev와 분리하고 값을 출력하지 않은 채 숫자 버전과 참조만 감사합니다.
2. Google·Kakao OAuth callback과 브라우저 키 제한이 `https://plimap.kr` 기준을 유지하는지 정기 확인합니다.
3. `PROD_PUBLIC_SMOKE_ENABLED=true`를 유지하고 프론트, CSRF, OAuth, 쿠키와 차단 경로를 매 배포 검증합니다.
4. Cloud Run 5xx·p95·인스턴스, Cloud SQL 연결·저장소·백업·PITR, Redis, 가동 상태과 비용 알림을 모니터링합니다.
5. 재해 복구에서는 애플리케이션 리비전과 Managed TLS가 준비된 뒤에만 DNS를 전환하고 공개 경로 점검를 다시 활성화합니다.
