# Prod external infrastructure apply runbook

이 문서는 이슈 #213의 영구 Prod 외부 인프라를 생성하기 직전과 생성 중에 사용하는 실행 기준이다. 명령 예시는 승인 범위를 설명하기 위한 것으로, PostgreSQL 17 임시 검증과 영구 Apply 승인을 각각 통과하기 전에는 실행하지 않는다.

## Apply gates

1. 별도 승인된 임시 Cloud SQL PostgreSQL 17에서 PostGIS 3.5.2 설치 가능 여부와 명시적 설치, 전체 Flyway Migration, runtime CRUD와 권한 음성 테스트를 완료한다.
2. 아래 영구 리소스 목록과 최신 월 비용을 다시 제시하고 영구 Apply 승인을 받는다.
3. 동명 리소스가 이미 있지만 설정이 다르면 수정하거나 재생성하지 않고 중단한다.
4. Secret payload, DB password, OAuth client secret과 Redis credential은 명령행 인자, 저장소, 이슈 또는 로그에 넣지 않는다.

임시 검증은 영구 Prod 인스턴스를 2시간 운용하거나 비용을 추정하는 작업이 아니다. 최소 사양의 별도 인스턴스를 최대 20분만 만들어 Cloud SQL이 그 시점에 제공하는 확장과 권한 동작을 확인하고 즉시 삭제하는 호환성 게이트다.

## Fixed resource plan

| Resource | Target |
| --- | --- |
| Project / number | `plimap` / `902362979890` |
| Region | `asia-northeast3` |
| VPC | `plimap-prod-vpc`, custom mode |
| Cloud Run subnet | `plimap-prod-run`, `10.20.0.0/24`, Private Google Access enabled |
| Private services range | `google-managed-services-plimap-prod-vpc`, `10.21.0.0/24` |
| Cloud SQL | `plimap-prod-postgres`, PostgreSQL 17, Enterprise, resulting tier `db-custom-1-3840` |
| Database | `plimap_prod` |
| Cloud Run | `plimap-api-prod`; sample bootstrap 1 vCPU/512 MiB, min 0/max 1; application target 1 vCPU/1 GiB, min 0/max 3 |
| Frontend Cloud Run | `plimap-web-prod`; 1 vCPU/512 MiB, min 0/max 2, request-based billing |
| Runtime service account | `plimap-api-prod@plimap.iam.gserviceaccount.com` |
| Deployer service account | `plimap-github-prod-deployer@plimap.iam.gserviceaccount.com` |
| Frontend service accounts | `plimap-web-prod@plimap.iam.gserviceaccount.com`, `plimap-web-prod-deployer@plimap.iam.gserviceaccount.com` |
| WIF | pool `github-actions`, providers `plimap-be-prod`, `plimap-web-prod` |
| GCS | `plimap-prod-profile-images`, Standard, uniform bucket-level access |
| Load Balancer | Global External Application Load Balancer, Premium Tier, IPv4 only, CDN/Cloud Armor disabled initially |
| Public IPv4 | `plimap-prod-lb-ip` / `8.233.213.169` |
| TLS | Google-managed certificate `plimap-prod-cert` for `plimap.kr` |

Applied through 2026-08-08: the VPC, subnet, private services range, service accounts, two WIF providers, bucket, 12 Secret containers, permanent private-IP Cloud SQL instance, API·frontend Cloud Run bootstrap services and the External Application Load Balancer exist in project `plimap`. Cloud SQL runs PostgreSQL 17.10 with PostGIS 3.5.2 and 18 successful Flyway migrations. Ten Secret IDs have enabled versions; only the two Kakao Secret payloads remain operator inputs.

The Cloud Run subnet is larger than the Direct VPC minimum `/26` and leaves room for revision overlap. The private services range is a separate `/24`; Cloud SQL consumes a `/24` per region and database type. Neither range overlaps the current default VPC subnet in `asia-northeast3` (`10.178.0.0/20`).

### GCE decision

Do not create a GCE instance or reuse the personal server's Traefik for Prod. The approved topology uses the Global External Application Load Balancer so the frontend and API stay in the same GCP project and share `https://plimap.kr`. Dev continues to use the personal server Traefik independently.

## Permanent Cloud SQL specification

| Setting | Value |
| --- | --- |
| Edition / machine | Enterprise / 1 vCPU, 3840 MiB (`db-custom-1-3840`) |
| Availability | Zonal |
| Storage | SSD 10 GiB, auto increase enabled, maximum 50 GiB |
| Network | Private IP only on `plimap-prod-vpc` |
| Connector enforcement | `NOT_REQUIRED`, because the app uses direct private-IP JDBC |
| TLS | `ENCRYPTED_ONLY`; JDBC uses `sslmode=require` |
| Backup | Daily at 15:00 UTC, retained backups 8 |
| PITR | Enabled, transaction logs retained 7 days |
| Maintenance | Saturday 19:00 UTC, Sunday 04:00 KST |
| Protection | Deletion protection enabled |

Use `--cpu=1 --memory=3840MiB` with the current stable gcloud CLI. The resulting Enterprise custom tier is `db-custom-1-3840`. Do not pass the MySQL-only `--enable-auto-upgrade-minor-version` flag for PostgreSQL.

## Estimated monthly baseline

This estimate must be refreshed immediately before permanent Apply. It is not an invoice or a hard cap.

| Component | Initial estimate |
| --- | ---: |
| Cloud SQL Seoul zonal compute, 1 vCPU/3.75 GiB, 730 h | about USD 64.11 |
| Cloud SQL SSD 10 GiB | about USD 2.21 |
| Secret Manager, 12 one-version secrets | up to about USD 0.72 |
| GCS Standard Seoul, example 10 GiB | about USD 0.22 |
| Redis Cloud selected plan | about USD 7.00 |
| Global external LB forwarding-rule baseline, first five rules | about USD 18.25 |
| VPC/Private Service Access/Cloud Run min 0/GCE | USD 0 fixed idle baseline |
| **Initial total** | **about USD 92-95/month** |

Backup storage beyond the included allowance, Cloud Run active instance time, Artifact Registry storage, Load Balancer traffic processing, GCS/Redis/Cloud Run egress and monitoring traffic are variable. The review threshold is USD 110/month and the project budget alert target should be raised from USD 80 to at least USD 100 at 50%, 75%, 90% and 100%. Budget alerts do not stop resources automatically.

## Apply order

### 1. Recheck collisions and enable APIs

Re-list the VPC, subnets, allocated ranges, Cloud SQL, Cloud Run, GCS, service accounts, WIF providers and Prod Secret IDs. Only after an approved clean result, enable the APIs required by the resources:

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

### 2. Create the isolated VPC and Private Service Access

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

The existing Cloud Run service agent already has `roles/run.serviceAgent`, which contains the Direct VPC permissions. Do not add a duplicate project-wide `roles/compute.networkUser` binding to it. Grant the GitHub deployer `roles/compute.networkUser` only on `plimap-prod-run`, so it can keep the approved subnet on new revisions without receiving access to other subnets.

### 3. Create service accounts and the Prod WIF provider

Create only these two service accounts. Do not grant the deployer project-wide `roles/run.admin` or Secret payload access.

The provider condition is:

```text
assertion.repository == 'UMC10th-PLIMAP/plimap-be'
&& assertion.ref == 'refs/heads/main'
&& assertion.environment == 'production'
&& assertion.workflow_ref ==
   'UMC10th-PLIMAP/plimap-be/.github/workflows/deploy-prod.yml@refs/heads/main'
```

Map `google.subject`, `attribute.repository`, `attribute.ref`, `attribute.environment` and `attribute.workflow_ref`. Bind `roles/iam.workloadIdentityUser` on the Prod deployer only to the pool principal set whose `attribute.repository` is `UMC10th-PLIMAP/plimap-be`. Do not change the existing Dev provider `plimap-be`.

### 4. Create the GCS bucket and bucket IAM

```powershell
gcloud storage buckets create gs://plimap-prod-profile-images `
  --project=plimap `
  --location=asia-northeast3 `
  --default-storage-class=STANDARD `
  --uniform-bucket-level-access `
  --soft-delete-duration=0 `
  --no-public-access-prevention
```

Verify versioning is disabled. Grant the runtime service account `roles/storage.objectUser`. Grant `allUsers` bucket-level `roles/storage.legacyObjectReader`, then verify an existing known object can be fetched anonymously and an anonymous object listing fails. If an organization Public Access Prevention policy blocks the binding, stop; do not substitute `roles/storage.objectViewer`, because that also grants list access.

### 5. Create Secret containers and least-privilege IAM

Create the 12 Secret IDs in `SECRETS.md` without payload versions. For each Secret:

- Runtime service account: `roles/secretmanager.secretAccessor`.
- Prod deployer: `roles/secretmanager.viewer` for version metadata only.

Then verify the deployer can describe `latest` metadata after a version exists but cannot run `secrets versions access`. It must not have any Dev Secret binding.

### 6. Create permanent Cloud SQL

Run only after the final PG17 gate records the live default, confirms PostGIS 3.5.2 is an available install version, and installs exactly 3.5.2:

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

After creation, verify there is no public IP, the private network is exact, backup/PITR/deletion protection settings match, and the effective database version and PostGIS extension version still match the approved gate.

### 7. Bootstrap database roles

Set the built-in administrator password and the two dedicated role passwords through a no-history operator path. Follow `PROD_DATABASE.md` and run, in order:

1. `bootstrap-prod-database.sql` as a Cloud SQL administrator against `plimap_prod`.
2. `configure-prod-database-grants.sql` as `plimap_migrator`.
3. Store the JDBC URL without credentials and the two role credential pairs in their separate Secret versions.
4. Verify the final Flyway Migration revoked all `plimap_app` access to `flyway_schema_history`. Run the grant script again only for tables or sequences that predate the migrator ownership; it repeats the revoke as defense in depth.

### 8. Apply remaining IAM and bootstrap Cloud Run

Grant the Prod deployer:

- `roles/artifactregistry.writer` on repository `plimap-docker`.
- `roles/iam.serviceAccountUser` on the Prod runtime service account.
- `roles/compute.networkUser` on subnet `plimap-prod-run`.
- Secret metadata viewer on each Prod Secret.

Then run `bootstrap-prod-cloud-run.ps1 -VpcNetwork plimap-prod-vpc -VpcSubnet plimap-prod-run -Apply` as the infrastructure administrator. Cloud Run requires a new service's first revision to receive traffic, so the script leaves the sample as the only 100% bootstrap revision while disabling the default URL and limiting ingress to Cloud Load Balancing. It grants public Invoker once and grants the deployer service-level `roles/run.developer`. This does not perform the first application deployment.

### 9. Configure GitHub Environment metadata

Set these non-secret `production` Environment variables:

```text
PROD_PUBLIC_BASE_URL=https://plimap.kr
PROD_FRONTEND_REDIRECT_URI=https://plimap.kr/app/oauth/callback
PROD_CORS_ALLOWED_ORIGINS=https://plimap.kr
PROD_OAUTH_ALLOWED_FRONTEND_ORIGINS=https://plimap.kr
PROD_GCS_BUCKET=plimap-prod-profile-images
PROD_VPC_NETWORK=plimap-prod-vpc
PROD_VPC_SUBNET=plimap-prod-run
PROD_PUBLIC_SMOKE_ENABLED=false
```

Keep `main` as the only deployment branch. The selected policy allows the deploying owner to be the sole required reviewer with prevent-self-review disabled, so a different team reviewer is not mandatory. Tighten this later if the team adopts separation of duties.

## Post-Apply verification

- Cloud SQL: Private IP only, PostGIS/Flyway/CRUD/least-privilege evidence, backup and PITR state.
- Cloud Run: API·frontend services use `internal-and-cloud-load-balancing`, default URLs are disabled, and only bootstrap revisions serve traffic until the release.
- IAM: deployer cannot access Secret payloads or modify `plimap-api-dev`; runtime can access only Prod Secret payloads and the Prod bucket.
- GCS: anonymous GET succeeds for a known object, anonymous list fails, runtime upload/delete succeeds.
- WIF: only the exact main/production/deploy-prod workflow can impersonate the Prod deployer.
- Artifact Registry: capture live and rollback digests before cleanup-policy dry-run; do not activate deletion without a separate review.
- Recovery: perform a separately cost-approved PITR restore rehearsal, verify PostGIS and Flyway history, and delete the restore instance.
- Load Balancer: HTTP redirects to HTTPS, `/api/**`·`/oauth/**` target the API NEG, all other paths target the frontend NEG, and the managed certificate stays provisioning until DNS points to the LB.
- Monitoring: Cloud Run 5xx/p95/max-instance, Cloud SQL CPU/memory/connections/disk, Redis usage/connectivity, external uptime and budget alerts.

## Operator follow-up outside current GCP access

These items require user or external-service access unless credentials are explicitly delegated:

1. Enter enabled versions for `plimap-prod-kakao-rest-api-key` and `plimap-prod-kakao-rest-api-secret` without exposing their payloads in chat or shell history.
2. Add the frontend GitHub `production` Environment secrets `PROD_VITE_GOOGLE_MAPS_API_KEY`, `PROD_VITE_GOOGLE_MAPS_MAP_ID`, `PROD_VITE_KAKAO_REST_API_KEY`; restrict browser keys to `https://plimap.kr` at each provider.
3. Confirm Google and Kakao Prod OAuth callbacks are registered as `https://plimap.kr/oauth/callback/{provider}`.
4. Merge frontend PR #247 and the backend #213 PR into `develop`, then merge each repository's reviewed `develop -> main` release so the actual Cloud Run revisions deploy.
5. Only after both application revisions are ready, create the Gabia apex A record (`@` or blank host) pointing to `8.233.213.169` with an initial TTL of 300.
6. Wait for `plimap-prod-cert` to become `ACTIVE`, set both repositories' `PROD_PUBLIC_SMOKE_ENABLED=true`, and verify frontend, CSRF, OAuth, cookie and blocked paths end to end.
7. Choose team-owned alert destinations and have a billing administrator create or raise the monthly budget alert to at least USD 100. The current project access is insufficient for billing-account budget creation.

DNS activation remains the final user-traffic gate; do not point `plimap.kr` at the LB while either service still serves its bootstrap revision.
