# Code Conventions

---

## Git Conventions

## Issue

### 제목 형식

```text
[Type] 변경 내용
```

### Issue 유형

| Type | 설명 | 제목 예시 |
|------|------|-----------|
| `Bug` | 오류 또는 예상과 다르게 동작하는 문제 | `[Bug] 로그인 실패 오류` |
| `Feature` | 새로운 기능 추가 | `[Feature] 회원가입 API 구현` |
| `Task` | 설정, 유지보수 등 일반 작업 | `[Task] 개발 환경 설정` |
| `Document` | 문서 작성 및 수정 | `[Document] API 명세 추가` |
| `Refactor` | 기능 변경 없는 코드 구조 개선 | `[Refactor] 토큰 검증 로직 분리` |

### 규칙

1. 작업을 시작하기 전에 이슈를 먼저 생성합니다.
2. 이슈 템플릿의 필수 항목과 완료 조건을 작성합니다.
3. 하나의 이슈에는 하나의 작업 목적만 포함합니다.
4. 작업 성격에 맞는 라벨을 지정합니다.

---

## Branch Naming

### 브랜치 구조

```text
main                        # 운영 브랜치
develop                     # 개발 브랜치
<type>/<설명>-#<issue>      # 작업 브랜치
```

### 형식

```text
<type>/<간단한_설명>-#<issue_number>
```

| 구성요소 | 설명 | 예시 |
|----------|------|------|
| `type` | 작업 유형 | `feat`, `fix`, `chore`, `docs`, `refactor` |
| `설명` | 간단한 작업 설명 (영문, 케밥케이스) | `signup-api`, `image-upload` |
| `issue_number` | GitHub 이슈 번호 **(필수)** | `#14`, `#23`, `#108` |

### 브랜치 유형

| Type | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `chore` | 설정, 유지보수 등 일반 작업 |
| `docs` | 문서 작성 및 수정 |
| `refactor` | 코드 리팩터링 |
| `test` | 테스트 코드 추가 및 수정 |
| `hotfix` | 운영 환경의 긴급 수정 |

### 예시

```bash
feat/signup-api-#14          # 회원가입 API 기능 추가
fix/image-upload-#23         # 이미지 업로드 버그 수정
chore/docker-setup-#5        # Docker 환경 설정
docs/api-guide-#12           # API 문서 작성
refactor/token-logic-#8      # 토큰 검증 로직 리팩터링
```

### 규칙

1. 작업 브랜치는 `develop`에서 생성합니다.
2. 브랜치명에는 이슈 번호를 반드시 포함합니다.
3. 설명은 영문 소문자와 하이픈(`-`)을 사용합니다.
4. 하나의 브랜치는 하나의 이슈를 기준으로 작업합니다.

---

## Commit Message

### 형식

```text
<type>: <subject>
```

### 커밋 유형

| Type | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 작성 및 수정 |
| `refactor` | 코드 리팩터링 |
| `test` | 테스트 코드 추가 및 수정 |
| `chore` | 빌드 설정, 의존성, 코드 포맷팅 등 기타 작업 |
| `rename` | 파일 또는 폴더 이름 변경 및 이동 |
| `remove` | 파일 삭제 |

### 규칙

1. **type**: 소문자 영문으로 작성합니다.
2. **subject**: 한글 또는 영문으로 50자 이내로 작성하고 마침표를 붙이지 않습니다.
3. 하나의 커밋에는 하나의 논리적인 변경만 포함합니다.

### 예시

```bash
# 간단한 커밋
git commit -m "feat: 회원가입 API 구현"

# 버그 수정
git commit -m "fix: 이미지 업로드 시 NPE 발생 수정"

# 문서 수정
git commit -m "docs: API 사용 방법 추가"
```

---

## Pull Request

### 제목 형식

```text
[Type] 변경 내용
```

| Type | 설명 | 제목 예시 |
|------|------|-----------|
| `Feature` | 새로운 기능 추가 | `[Feature] 회원가입 API 구현` |
| `Bug` | 버그 수정 | `[Bug] 이미지 업로드 오류 수정` |
| `Task` | 설정, 유지보수 등 일반 작업 | `[Task] Docker 환경 설정` |
| `Document` | 문서 작성 및 수정 | `[Document] API 명세 추가` |
| `Refactor` | 코드 리팩터링 | `[Refactor] 토큰 검증 로직 분리` |

### 규칙

1. 작업 브랜치에서 `develop` 브랜치로 PR을 생성합니다.
2. PR 본문에 변경 내용과 테스트 결과를 작성합니다.
3. 관련 이슈는 `Closes #14` 로 연결합니다.
4. PR에는 하나의 이슈에 해당하는 변경만 포함합니다.
5. 리뷰 반영 후 해결된 리뷰 스레드를 정리합니다.

---

## Issue & PR 연동

- 브랜치 생성 전에 이슈를 먼저 생성합니다.
- 브랜치명에 이슈 번호를 반드시 포함합니다.
- PR 머지 시 이슈가 자동 종료되도록 `Closes #14` 를 작성합니다.
