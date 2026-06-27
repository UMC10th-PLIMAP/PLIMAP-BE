# PLIMAP-BE Code Review Guide

> 이 문서는 현재 확정된 최소 리뷰 규칙을 정의합니다. 프로젝트 구조와 공통 구현 방식이 결정되면 점진적으로 보완합니다.

---

## Review Style

- **모든 리뷰 코멘트는 한국어(ko-KR)로 작성합니다.**
- 자동 PR 요약을 제외한 리뷰 코멘트에는 일반적인 요약, 변경 설명, 칭찬을 작성하지 않습니다.
- PR에서 새로 추가되거나 변경된 코드에 직접 관련된 문제만 지적합니다.
- 코드에서 확인할 수 없는 의도를 추측하거나 근거 없이 광범위한 영향을 언급하지 않습니다.
- 구체적이고 객관적이며 실제로 수정할 수 있는 문제만 제시합니다.
- 문제를 지적할 때 발생 조건, 예상 영향, 수정 방향을 함께 설명합니다.
- 같은 원인에서 발생한 문제는 가능한 한 하나의 코멘트로 정리합니다.
- 개인 취향이나 동작에 영향이 없는 사소한 스타일 차이는 지적하지 않습니다.

### Review Priority

다음 우선순위에 따라 리뷰합니다.

1. **Correctness**: 잘못된 동작, 누락된 예외 처리, 경계 조건
2. **Security**: 인증·인가, 입력값 검증, 민감 정보 노출
3. **Data Integrity**: 트랜잭션, 연관관계, 데이터 제약 조건
4. **Performance**: N+1 쿼리, 불필요한 반복 조회, 과도한 연산
5. **Maintainability**: 계층 책임, 중복 코드, 지나치게 복잡한 구현

---

## Architecture

현재 프로젝트는 다음 기본 계층 구조를 따릅니다.

```text
Controller → Service → Repository → Entity
```

| Layer | Responsibility |
|-------|----------------|
| Controller | API 진입점, 요청 검증, 요청·응답 변환 |
| Service | 비즈니스 로직과 트랜잭션 관리 |
| Repository | 데이터 접근과 영속성 처리 |
| Entity | 도메인 상태와 상태 변경 행위 |

### Layer Rules

- 상위 계층에서 하위 계층 방향으로만 의존합니다.
- Controller에서 Repository를 직접 호출하지 않습니다.
- Controller에 비즈니스 로직을 작성하지 않습니다.
- Repository에 API 응답 생성이나 비즈니스 흐름을 작성하지 않습니다.
- 계층 간 순환 의존성을 만들지 않습니다.
- 아직 확정되지 않은 CQRS, Service Interface/Impl 분리 등의 구조를 임의로 강제하지 않습니다.

---

## Naming Conventions

### Package

- 소문자만 사용합니다.
- 도메인 이름은 단수형을 사용합니다.

```java
// Good
com.example.plimap.domain.member

// Bad
com.example.plimap.domain.Members
```

### Class

클래스의 역할을 알 수 있는 접미사를 사용합니다.

| Type | Suffix | Example |
|------|--------|---------|
| Controller | `Controller` | `MemberController` |
| Service | `Service` | `MemberService` |
| Repository | `Repository` | `MemberRepository` |
| Entity | 없음 | `Member` |
| Request DTO | `Request` | `MemberRequest` |
| Response DTO | `Response` | `MemberResponse` |
| Exception | `Exception` | `MemberNotFoundException` |
| Configuration | `Config` | `SecurityConfig` |

### Method

- camelCase를 사용합니다.
- 수행하는 동작을 나타내는 동사로 시작합니다.

| Action | Prefix | Example |
|--------|--------|---------|
| 단건 조회 | `get`, `find` | `getMember()`, `findById()` |
| 목록 조회 | `get`, `find`, `search` | `getMembers()`, `searchMembers()` |
| 생성 | `create`, `save` | `createMember()` |
| 수정 | `update`, `modify` | `updateMember()` |
| 삭제 | `delete`, `remove` | `deleteMember()` |
| 검증 | `validate`, `check`, `is` | `validateEmail()`, `isActive()` |

### Variable

- camelCase를 사용합니다.
- 변수의 역할과 대상을 알 수 있는 이름을 사용합니다.
- 문맥을 알 수 없는 `m`, `data`, `list` 등의 이름을 피합니다.

```java
// Good
Member member;
List<Member> activeMembers;
Long memberId;

// Bad
Member m;
List<Member> list;
Long value;
```

---

## Java and Spring Boot

- Java 21과 현재 프로젝트의 Spring Boot 버전에 맞는 표준 패턴을 사용합니다.
- 예외를 무시하거나 `Exception`으로 지나치게 포괄적으로 처리하지 않습니다.
- raw type 대신 명확한 제네릭 타입을 사용합니다.
- 의미가 불분명한 매직 넘버와 문자열은 상수 또는 Enum으로 관리합니다.
- 한 메서드가 여러 책임을 가지거나 지나치게 길어지면 역할별 분리를 검토합니다.
- Lombok 사용 시 Entity의 `equals`, `hashCode`, `toString`이 연관관계 순환이나 지연 로딩을 유발하지 않는지 확인합니다.

---

## Entity and JPA

### Entity Rules

- JPA 기본 생성자는 `protected` 접근 수준을 사용합니다.
- Entity에 공개 `@Setter`를 사용하지 않고, 의미 있는 비즈니스 메서드로 상태를 변경합니다.
- Enum 필드는 `@Enumerated(EnumType.STRING)`으로 저장합니다. `ORDINAL`은 사용하지 않습니다.
- Entity를 API 응답으로 직접 노출하지 않습니다.
- 연관관계의 영속성 전이와 고아 객체 제거가 의도하지 않은 데이터 변경을 만들지 않는지 확인합니다.

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status;

    public void updateStatus(MemberStatus status) {
        this.status = status;
    }
}
```

### Query Rules

- `Optional.get()`을 직접 호출하지 않고 `orElseThrow()` 등으로 값이 없는 경우를 처리합니다.
- N+1 쿼리와 반복문 안에서 실행되는 추가 쿼리를 확인합니다.
- 지연 로딩 데이터가 트랜잭션 밖에서 접근되지 않는지 확인합니다.
- 불필요한 전체 Entity 조회 대신 필요한 범위만 조회합니다.
- `nullable`, `unique`, 길이, 인덱스 등 DB 제약 조건이 도메인 규칙과 일치하는지 확인합니다.
- 쿼리와 매핑이 PostgreSQL에서 정상적으로 동작하는지 확인합니다.

```java
Member member = memberRepository.findById(memberId)
        .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
```

---

## Transaction

- 생성, 수정, 삭제와 같이 데이터를 변경하는 작업의 트랜잭션 누락을 확인합니다.
- 조회 전용 로직에는 필요한 경우 `@Transactional(readOnly = true)`를 사용합니다.
- 트랜잭션 범위가 비즈니스 작업의 원자성과 일치하는지 확인합니다.
- 트랜잭션 내부에서 불필요하게 오래 실행되는 외부 API 또는 파일 작업을 피합니다.
- 예외를 무시하여 실패한 작업이 커밋되지 않도록 확인합니다.

---

## API and Security

- 요청 DTO에 필요한 입력값 검증이 적용되었는지 확인합니다.
- 인증된 사용자와 요청 대상 리소스 사이의 인가 검사가 누락되지 않았는지 확인합니다.
- API 상황에 적합한 HTTP 상태 코드와 오류 응답을 사용합니다.
- 기존 API 계약을 깨는 변경은 PR에서 명확하게 설명되어야 합니다.
- 비밀번호, 토큰, 개인정보가 코드, 설정, 로그 또는 API 응답에 노출되지 않도록 합니다.
- SQL Injection, IDOR, 권한 상승 등 외부 입력과 접근 제어 취약점을 우선적으로 확인합니다.

---

## Testing

- 기능 변경에는 정상 흐름과 주요 실패·경계 조건에 대한 테스트를 포함합니다.
- 버그 수정에는 가능하면 문제를 재현하고 재발을 방지하는 테스트를 추가합니다.
- 구현 세부사항보다 외부에서 관찰 가능한 동작을 검증합니다.
- 테스트가 실행 순서나 외부 환경에 불필요하게 의존하지 않는지 확인합니다.

---

## Common Anti-Patterns

다음 패턴은 리뷰에서 우선적으로 확인합니다.

| Anti-Pattern | Review Direction |
|--------------|------------------|
| Entity의 공개 `@Setter` | 의도를 나타내는 상태 변경 메서드로 대체 |
| Controller에서 Repository 직접 호출 | Service를 통해 접근 |
| `Optional.get()` 직접 호출 | 값이 없는 경우를 명시적으로 처리 |
| 변경 작업의 트랜잭션 누락 | 적절한 트랜잭션 경계 설정 |
| `EnumType.ORDINAL` 사용 | `EnumType.STRING`으로 변경 |
| raw type 사용 | 명확한 제네릭 타입 지정 |
| 의미 없는 변수명 | 역할과 대상을 나타내는 이름 사용 |
| 매직 넘버와 문자열 | 상수 또는 Enum으로 관리 |
| 과도하게 긴 메서드 | 단일 책임에 맞게 분리 |
| 민감 정보 출력 | 로그와 응답에서 제거 또는 마스킹 |

---

## Project Conventions

- Issue, Branch, Commit, Pull Request 규칙은 `docs/CONVENTION.md`를 따릅니다.
- PR의 변경 범위가 연결된 이슈의 목적을 벗어나면 이를 명시합니다.
- 프로젝트에 아직 도입되지 않은 라이브러리나 구조를 근거 없이 요구하지 않습니다.
- 새로운 아키텍처나 공통 패턴이 확정되면 이 가이드에 먼저 반영한 후 리뷰 기준으로 사용합니다.
