# 정지·탈퇴 회원 로그인/세션 처리 — 프론트 연동 가이드

> 관련 이슈: [#301](https://github.com/UMC10th-PLIMAP/plimap-be/issues/301)(PR [#303](https://github.com/UMC10th-PLIMAP/plimap-be/pull/303)), [#304](https://github.com/UMC10th-PLIMAP/plimap-be/pull/306)

정지(SUSPENDED)·탈퇴(WITHDRAWN) 회원에게 제재 사유·상태·해제일을 보여줄 때, **회원이 이미 유효한 토큰을 갖고 있었는지 여부**에 따라 두 가지 경로 중 하나로 정보가 전달됩니다.

| 상황 | 전달 경로 | 이유 |
|---|---|---|
| 정지/탈퇴된 계정이 **새로 로그인을 시도**할 때 | OAuth 리다이렉트 **쿼리스트링** | 로그인 자체가 차단되어 토큰이 발급되지 않으므로, API를 호출할 방법이 없음 |
| **이미 로그인해서 유효한 토큰**을 갖고 있던 계정이 이후 정지/탈퇴될 때 | `GET /api/v1/members/me` **JSON 응답** | 토큰이 살아있는 동안은 이 엔드포인트만 예외적으로 호출 허용됨 |

두 경로 모두 같은 정보(제재 카테고리·상세 사유·정지 해제일·제재 기간·누적 벌점)를 전달하지만, 필드 이름과 형식이 다릅니다.

---

## 1. 로그인 시도 시점 — OAuth 리다이렉트 쿼리스트링

이미 정지 중이거나(SUSPENDED, 미만료) 벌점 누적으로 자동/영구 탈퇴된(WITHDRAWN, PENALTY) 계정이 카카오/구글 로그인을 시도하면, 백엔드가 로그인 자체를 막고 프론트 콜백 URL로 리다이렉트합니다. 이 경우 **JWT는 발급되지 않습니다.**

자발적 탈퇴(VOLUNTARY) 회원은 이 경로에 해당하지 않습니다 — 소셜 계정이 하드 삭제되어 재로그인 시 완전히 새로운 회원으로 처리되기 때문에, 로그인 시점에는 감지할 수 없습니다(의도된 동작).

### 쿼리 파라미터

| 파라미터 | 값이 붙는 조건 | 설명 |
|---|---|---|
| `error` | 항상 | `account_suspended`(정지) 또는 `account_permanently_banned`(영구탈퇴) |
| `reasonCategory` | 제재 이력이 있으면 | `ReportCategory` — `PERSONAL_INFORMATION_EXPOSURE` / `OBSCENE_OR_HARMFUL` / `ABUSE_OR_HATE_SPEECH` / `COMMERCIAL_OR_PROMOTIONAL` / `OTHER` |
| `reasonDetail` | `reasonCategory`가 `OTHER`일 때만 | 자유 텍스트 사유(URL-encoded, UTF-8) |
| `suspendedUntil` | 정지 중일 때만(ISO-8601 Instant) | 영구탈퇴 시에는 항상 없음(파라미터 자체가 생략됨) |
| `period` | 제재 이력이 있으면 | `SuspensionPeriod` — `ONE_DAY` / `THREE_DAYS` / `FIVE_DAYS` / `PERMANENT` |
| `penaltyPoint` | 항상 | 누적 벌점(이 상태에 도달했다는 건 제재 이력이 있다는 뜻이라 항상 1 이상) |

값이 `null`인 파라미터는 URL에서 아예 생략됩니다(빈 문자열로 붙지 않음).

### 예시

**1일 정지(1회차) — 로그인 시도**
```
https://dev.plimap.kr/app/oauth/callback?error=account_suspended&reasonCategory=COMMERCIAL_OR_PROMOTIONAL&suspendedUntil=2026-08-14T14:30:00Z&period=ONE_DAY&penaltyPoint=1
```

**3일 정지(2회차) — 로그인 시도**
```
https://dev.plimap.kr/app/oauth/callback?error=account_suspended&reasonCategory=ABUSE_OR_HATE_SPEECH&suspendedUntil=2026-08-16T14:30:00Z&period=THREE_DAYS&penaltyPoint=2
```

**5일 정지(3회차) — 로그인 시도**
```
https://dev.plimap.kr/app/oauth/callback?error=account_suspended&reasonCategory=OBSCENE_OR_HARMFUL&suspendedUntil=2026-08-18T14:30:00Z&period=FIVE_DAYS&penaltyPoint=3
```

**영구정지(4회차) — 로그인 시도**
```
https://dev.plimap.kr/app/oauth/callback?error=account_permanently_banned&reasonCategory=OTHER&reasonDetail=%EB%B0%98%EB%B3%B5%EC%A0%81%EC%9D%B8%20%EC%BB%A4%EB%AE%A4%EB%8B%88%ED%8B%B0%20%EA%B0%80%EC%9D%B4%EB%93%9C%EB%9D%BC%EC%9D%B8%20%EC%9C%84%EB%B0%98%EC%9C%BC%EB%A1%9C%20%EC%98%81%EA%B5%AC%20%EC%A0%9C%EC%9E%AC&period=PERMANENT&penaltyPoint=4
```
(`reasonDetail` 디코딩 결과: `반복적인 커뮤니티 가이드라인 위반으로 영구 제재`)

영구정지는 `suspendedUntil`이 `null`로 초기화되기 때문에 파라미터 자체가 없습니다.

---

## 2. 세션 중 조회 — `GET /api/v1/members/me`

이미 로그인해서 유효한 토큰을 갖고 있던 회원이 나중에 정지/탈퇴되어도, `MemberStatusInterceptor`가 `GET`/`DELETE /api/v1/members/me`는 예외적으로 허용합니다. 이 토큰으로 내 프로필을 조회하면 200과 함께 제재 정보가 담긴 JSON이 내려옵니다.

**전역 에러 처리 흐름 제안**: 다른 API 호출 중 `MEMBER_403_SUSPENDED`/`MEMBER_403_WITHDRAWN` 에러코드를 받으면(이미 세션이 있는 상태에서 제재당한 경우), 이 시점에 `GET /api/v1/members/me`를 호출해 구조화된 상세 정보를 가져와 안내 모달에 표시하면 됩니다.

### 응답 필드 (`MemberResponse.MyProfile`)

| 필드 | 타입 | 설명 |
|---|---|---|
| `status` | `MemberStatus` | `ACTIVE` / `SUSPENDED` / `WITHDRAWN` |
| `suspendedUntil` | `Instant` 또는 `null` | 정지 해제 시각. WITHDRAWN이거나 제재 이력이 없으면 `null` |
| `withdrawalReason` | `WithdrawalReason` 또는 `null` | `VOLUNTARY`(자발적 탈퇴) / `PENALTY`(제재) / `null`(탈퇴 아님) |
| `reasonCategory` | `ReportCategory` 또는 `null` | 제재 이력이 없으면 `null` |
| `reasonDetail` | `String` 또는 `null` | `reasonCategory`가 `OTHER`일 때만 값 존재 |
| `penaltyPoint` | `int` | 누적 벌점(절대 리셋되지 않음). 제재 이력 없으면 `0` |
| `lastPenaltyPeriod` | `SuspensionPeriod` 또는 `null` | 가장 최근 제재 기간. 제재 이력 없으면 `null` |

**주의**: 정지가 해제(lazy 자동 해제)되어 `ACTIVE`로 복귀한 회원은 `suspendedUntil`만 `null`로 초기화되고, `penaltyPoint`/`lastPenaltyPeriod`/`reasonCategory`/`reasonDetail`은 마지막 제재 이력이 그대로 남아있습니다 — 즉 `status === "ACTIVE"`만으로 "제재 이력이 아예 없다"고 판단하면 안 되고, 필요하면 `penaltyPoint > 0` 여부로 판단하세요.

### 예시

**1일 정지(1회차)**
```json
{
  "isSuccess": true,
  "code": "MEMBER_MY_PROFILE_FETCHED_SUCCESS",
  "message": "내 프로필을 조회했습니다.",
  "result": {
    "id": 42,
    "nickname": "예림",
    "name": "이예림",
    "introduction": "안녕하세요",
    "profileImageUrl": "https://storage.plimap.kr/profile/42.webp",
    "followerCount": 12,
    "followingCount": 8,
    "onboardingCompletedAt": "2026-06-01T09:00:00Z",
    "pinCount": 5,
    "status": "SUSPENDED",
    "suspendedUntil": "2026-08-14T14:30:00Z",
    "withdrawalReason": null,
    "reasonCategory": "COMMERCIAL_OR_PROMOTIONAL",
    "reasonDetail": null,
    "penaltyPoint": 1,
    "lastPenaltyPeriod": "ONE_DAY"
  }
}
```

**3일 정지(2회차)**
```json
{
  "isSuccess": true,
  "code": "MEMBER_MY_PROFILE_FETCHED_SUCCESS",
  "message": "내 프로필을 조회했습니다.",
  "result": {
    "id": 42,
    "nickname": "예림",
    "name": "이예림",
    "introduction": "안녕하세요",
    "profileImageUrl": "https://storage.plimap.kr/profile/42.webp",
    "followerCount": 12,
    "followingCount": 8,
    "onboardingCompletedAt": "2026-06-01T09:00:00Z",
    "pinCount": 5,
    "status": "SUSPENDED",
    "suspendedUntil": "2026-08-16T14:30:00Z",
    "withdrawalReason": null,
    "reasonCategory": "ABUSE_OR_HATE_SPEECH",
    "reasonDetail": null,
    "penaltyPoint": 2,
    "lastPenaltyPeriod": "THREE_DAYS"
  }
}
```

**5일 정지(3회차)**
```json
{
  "isSuccess": true,
  "code": "MEMBER_MY_PROFILE_FETCHED_SUCCESS",
  "message": "내 프로필을 조회했습니다.",
  "result": {
    "id": 42,
    "nickname": "예림",
    "name": "이예림",
    "introduction": "안녕하세요",
    "profileImageUrl": "https://storage.plimap.kr/profile/42.webp",
    "followerCount": 12,
    "followingCount": 8,
    "onboardingCompletedAt": "2026-06-01T09:00:00Z",
    "pinCount": 5,
    "status": "SUSPENDED",
    "suspendedUntil": "2026-08-18T14:30:00Z",
    "withdrawalReason": null,
    "reasonCategory": "OBSCENE_OR_HARMFUL",
    "reasonDetail": null,
    "penaltyPoint": 3,
    "lastPenaltyPeriod": "FIVE_DAYS"
  }
}
```

**영구정지(4회차)**
```json
{
  "isSuccess": true,
  "code": "MEMBER_MY_PROFILE_FETCHED_SUCCESS",
  "message": "내 프로필을 조회했습니다.",
  "result": {
    "id": 42,
    "nickname": "플리맵사용자",
    "name": null,
    "introduction": null,
    "profileImageUrl": null,
    "followerCount": 0,
    "followingCount": 0,
    "onboardingCompletedAt": "2026-06-01T09:00:00Z",
    "pinCount": 0,
    "status": "WITHDRAWN",
    "suspendedUntil": null,
    "withdrawalReason": "PENALTY",
    "reasonCategory": "OTHER",
    "reasonDetail": "반복적인 커뮤니티 가이드라인 위반으로 영구 제재",
    "penaltyPoint": 4,
    "lastPenaltyPeriod": "PERMANENT"
  }
}
```

영구정지 시 `nickname`은 `플리맵사용자`로 마스킹되고 `name`/`introduction`/`profileImageUrl`은 전부 삭제된 상태로 내려갑니다.

---

## 참고: 관리자 제재 API (백엔드/QA용)

위 예시들은 관리자가 `POST /api/v1/admin/members/{memberId}/sanctions`를 호출해 만든 상태입니다.

```json
// 1일 정지
{ "category": "COMMERCIAL_OR_PROMOTIONAL", "detail": null, "period": "ONE_DAY" }

// 3일 정지
{ "category": "ABUSE_OR_HATE_SPEECH", "detail": null, "period": "THREE_DAYS" }

// 5일 정지
{ "category": "OBSCENE_OR_HARMFUL", "detail": null, "period": "FIVE_DAYS" }

// 영구정지
{ "category": "OTHER", "detail": "반복적인 커뮤니티 가이드라인 위반으로 영구 제재", "period": "PERMANENT" }
```

- `detail`은 `category`가 `OTHER`일 때만 값을 넣을 수 있고, 그 외 카테고리는 반드시 `null`이어야 함(위반 시 400)
- 이미 벌점 3점인 회원이 4번째 제재를 받으면, `period`로 무엇을 보내든 `penaltyPoint>=4` 도달로 자동 영구탈퇴 처리됨 — 이때도 응답의 `lastPenaltyPeriod`엔 요청에 담긴 `period` 값이 그대로 스냅샷됨
- 관리자 인증(JWT, `role=ADMIN`) 필요
