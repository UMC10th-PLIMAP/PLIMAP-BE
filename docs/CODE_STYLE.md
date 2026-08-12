# Code Style Guide

## Overview

이 문서는 PLIMAP API에서 팀원들이 동일한 방식으로 코드를 작성하기 위한 핵심 규칙을 정리합니다.

목표는 모든 스타일을 세세하게 제한하는 것이 아니라, 협업 과정에서 코드 구조가 흔들리거나 리뷰 기준이 달라지기 쉬운 부분을 통일하는 것입니다.

## Common Rules

### Naming

클래스명은 역할이 드러나도록 접미사를 붙입니다.

| 역할 | 이름 형식 | 예시 |
|------|-----------|------|
| Controller | `{Domain}Controller` | `PinController` |
| Swagger Docs | `{Domain}ControllerDocs` | `PinControllerDocs` |
| Command Service | `{Domain}CommandService` | `PinCommandService` |
| Command Service 구현체 | `{Domain}CommandServiceImpl` | `PinCommandServiceImpl` |
| Query Service | `{Domain}QueryService` | `PinQueryService` |
| Query Service 구현체 | `{Domain}QueryServiceImpl` | `PinQueryServiceImpl` |
| JPA Repository | `{Domain}Repository` | `PinRepository` |
| Query Repository | `{Domain}QueryRepository` | `PinQueryRepository` |
| Query Repository 구현체 | `{Domain}QueryRepositoryImpl` | `PinQueryRepositoryImpl` |
| Entity | 도메인 명사 | `Pin`, `Member`, `Place` |

메서드명은 기본적으로 다음 동사를 사용합니다.

| 목적 | 동사 | 예시 |
|------|------|------|
| 생성 | `create` | `createPin` |
| 수정 | `update` | `updatePin` |
| 삭제 | `delete` | `deletePin` |
| 단건 조회 | `get` | `getPin` |
| 조건 조회 | `find` | `findByEmail` |
| 검색 | `search` | `searchPlaces` |
| 존재 여부 | `exists` | `existsByEmail` |

단순 CRUD가 아니라 도메인 행위가 명확한 경우에는 행위 중심 이름을 사용합니다.

```java
bookmarkTrack(memberId, trackId);
cancelBookmark(memberId, trackId);
```


## Controller Layer

Controller는 HTTP 요청과 응답만 담당합니다.

Controller에서 처리할 책임은 다음으로 제한합니다.

- 요청 값 바인딩
- Bean Validation
- Service 호출
- 공통 응답 반환

Controller에서 비즈니스 로직을 처리하지 않습니다.

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/pins")
public class PinController implements PinControllerDocs {

    private final PinCommandService pinCommandService;
    private final PinQueryService pinQueryService;
}
```

### API Response

모든 API 응답은 공통 응답 형식을 사용합니다.

```java
ApiResponse<T>
```

응답 본문은 `isSuccess`, `code`, `message`, `result` 필드로 구성하며, 반환 데이터가 없을 때도 `result`는 `null`로 명시합니다. 실제 HTTP 상태는 코드의 `HttpStatus`를 사용해 `ResponseEntity`에 설정합니다.

데이터가 없는 성공 응답은 `ApiResponse<Void>`를 사용합니다.

```java
pinCommandService.deletePin(pinId);
return ResponseEntity
        .status(PinSuccessCode.PIN_DELETED.getStatus())
        .body(ApiResponse.success(PinSuccessCode.PIN_DELETED, null));
```

HTTP 상태 코드는 의미에 맞게 사용합니다.

| 상황 | HTTP Status |
|------|-------------|
| 조회 성공 | `200 OK` |
| 생성 성공 | `201 Created` |
| 요청 값 오류 | `400 Bad Request` |
| 인증 실패 | `401 Unauthorized` |
| 권한 없음 | `403 Forbidden` |
| 리소스 없음 | `404 Not Found` |
| 충돌 | `409 Conflict` |
| 서버 오류 | `500 Internal Server Error` |

### Swagger

Swagger 문서는 Controller에 직접 작성하지 않고 `controller/docs` 패키지의 인터페이스에 작성합니다.

```text
controller/
├── PinController.java
└── docs/
    └── PinControllerDocs.java
```

## DTO Layer

Request와 Response DTO는 분리합니다.

DTO는 기본적으로 `record`를 사용합니다.

```java
public class PinRequest {

    public record Create(
            Long placeId,
            Long trackId,
            String content
    ) {
    }
}
```

```java
public class PinResponse {

    public record Detail(
            Long id,
            String content
    ) {
        public static Detail from(Pin pin) {
            return new Detail(
                    pin.getId(),
                    pin.getContent()
            );
        }
    }
}
```

Response DTO 이름은 아래 기준을 우선 사용합니다.

| 이름 | 용도 |
|------|------|
| `Detail` | 단건 상세 |
| `Summary` | 요약 |
| `Item` | 목록 요소 |
| `Page` | 페이지 응답 |
| `Result` | 처리 결과 |

Entity를 API 응답으로 직접 노출하지 않습니다.

### Converter

단순한 Entity to DTO 변환은 Response DTO의 `from()` 메서드를 사용합니다.

여러 도메인의 데이터를 조합하거나 변환 로직이 길어지는 경우에만 `converter` 패키지에 별도 Converter를 둡니다.

## Service Layer

Service는 Command와 Query를 분리합니다.

```text
service/
├── command/
│   ├── PinCommandService.java
│   └── impl/
│       └── PinCommandServiceImpl.java
└── query/
    ├── PinQueryService.java
    └── impl/
        └── PinQueryServiceImpl.java
```

### Command Service

Command Service는 상태 변경을 담당합니다.

- 생성
- 수정
- 삭제
- 상태 변경

```java
public interface PinCommandService {

    PinResponse.Detail createPin(PinRequest.Create request);

    void deletePin(Long pinId);
}
```

```java
@Service
@RequiredArgsConstructor
@Transactional
public class PinCommandServiceImpl implements PinCommandService {
}
```

### Query Service

Query Service는 상태 변경 없는 조회를 담당합니다.

- 단건 조회
- 목록 조회
- 검색
- 필터링

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PinQueryServiceImpl implements PinQueryService {
}
```

### Domain Dependency

다른 도메인의 Repository를 직접 주입하는 것은 지양합니다.

다른 도메인의 기능이 필요하면 해당 도메인의 Service 인터페이스를 통해 호출합니다.

```java
private final PlaceQueryService placeQueryService;
private final TrackQueryService trackQueryService;
```

도메인 간 순환 호출이 생기지 않도록 주의합니다.

## Entity Layer

Entity에는 필요한 Lombok만 제한적으로 사용합니다.

```java
@Entity
@Table(name = "pin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pin extends SoftDeleteEntity {
}
```

| Annotation | 기준 |
|------------|------|
| `@Getter` | 허용 |
| `@NoArgsConstructor(access = PROTECTED)` | 필수 |
| `@Setter` | 금지 |
| `@Data` | 금지 |
| `@AllArgsConstructor` | 지양 |
| `@Builder` | private 생성자에만 사용 |

### Creation

Entity 생성은 private 생성자에 선언한 Builder를 사용합니다.

클래스 레벨 `@Builder`는 사용하지 않습니다.

```java
@Builder
private Pin(Member member, Place place, Track track, String content) {
    this.member = member;
    this.place = place;
    this.track = track;
    this.content = content;
}

public static Pin create(Member member, Place place, Track track, String content) {
    return Pin.builder()
            .member(member)
            .place(place)
            .track(track)
            .content(content)
            .build();
}
```

### State Change

Entity에 Setter를 열지 않습니다.

상태 변경은 의미 있는 메서드로 표현합니다.

```java
public void updateContent(String content) {
    this.content = content;
}
```

### Relationship

연관관계는 기본적으로 `LAZY`를 사용합니다.

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "member_id", nullable = false)
private Member member;
```

양방향 연관관계는 꼭 필요한 경우에만 사용합니다.

### Enum

Enum은 반드시 문자열로 저장합니다.

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private PinStatus status;
```

`EnumType.ORDINAL`은 사용하지 않습니다.

### Base Entity

모든 Entity는 공통 `BaseEntity`를 상속하며 다음 필드를 관리합니다.

- `createdAt`
- `updatedAt`

시간 필드는 PostgreSQL의 `TIMESTAMPTZ`와 일관되도록 `Instant`를 사용하며, 생성일과 수정일은 JPA Auditing으로 기록합니다.

삭제 이력 보존이 필요한 Entity만 `SoftDeleteEntity`를 상속합니다.

- 적용 대상: `Member`, `Place`, `PlaceTrack`, `Pin`
- 공통 필드: `deletedAt`
- 상태 변경: `delete()`, `restore()`

```java
pin.delete();
```

Soft Delete 대상에는 `repository.delete()`와 `repository.deleteById()`를 사용하지 않습니다. 일반 조회에는 `deletedAt IS NULL` 조건을 적용하고, 복구 유스케이스에서만 삭제된 엔티티를 별도로 조회하여 `restore()`를 호출합니다.

Soft Delete 대상이 아닌 이력·매핑 Entity는 도메인별 보존 요구사항에 따라 물리 삭제할 수 있습니다.

## Repository Layer

단순 CRUD와 단순 조건 조회는 Spring Data JPA 메서드를 사용합니다.

```java
public interface PinRepository extends JpaRepository<Pin, Long> {

    Optional<Pin> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByMemberIdAndPlaceIdAndDeletedAtIsNull(Long memberId, Long placeId);
}
```

동적 조건, 복잡한 검색, 페이징 조회는 QueryDSL을 사용합니다.

```text
repository/
└── query/
    ├── PinQueryRepository.java
    └── impl/
        └── PinQueryRepositoryImpl.java
```

```java
public interface PinQueryRepository {

    List<Pin> searchPins(PinSearchCondition condition);
}
```

QueryDSL 구현체는 공통 `JPAQueryFactory`를 생성하지 않고 생성자 주입으로 사용합니다.

```java
@Repository
@RequiredArgsConstructor
public class PinQueryRepositoryImpl implements PinQueryRepository {

    private final JPAQueryFactory queryFactory;
}
```

- 조회 인터페이스는 `{Domain}QueryRepository`, 구현체는 `{Domain}QueryRepositoryImpl`로 작성합니다.
- 동적 조건은 의미가 드러나는 private 메서드로 분리하고 조건이 없으면 `null`을 반환합니다.
- 목록 조회의 정렬 순서는 명시하며, 동일 값일 때 사용할 보조 정렬 키까지 지정합니다.
- Query Repository 안에서 트랜잭션을 시작하거나 비즈니스 상태를 변경하지 않습니다.

Query Repository는 API 응답 DTO에 의존하지 않습니다. 조회 결과를 API 응답으로 변환하는 책임은 Query Service 또는 Converter가 담당합니다.

JPQL, QueryDSL, Native Query 선택 기준은 다음과 같습니다.

| 방식 | 사용 기준 |
|------|-----------|
| Spring Data JPA | 단순 CRUD, 단순 조건 조회 |
| JPQL / EntityGraph | 정적인 fetch join |
| QueryDSL | 동적 조건, 복잡한 검색, 페이징 |
| Native Query | 다른 방식으로 해결하기 어려운 경우 |

Native Query를 사용할 때는 코드 리뷰에서 사용 이유를 설명합니다.

## Exception Layer

공통 응답 코드와 전역 예외 처리는 `global.apiPayload`에서 관리하고, 도메인 에러 코드와 예외는 각 도메인의 `exception` 패키지에서 관리합니다.

```text
global/apiPayload/
├── ApiResponse.java
├── code/
│   ├── BaseErrorCode.java
│   ├── BaseSuccessCode.java
│   ├── GeneralErrorCode.java
│   └── GeneralSuccessCode.java
└── exception/
    ├── BusinessException.java
    └── GlobalExceptionHandler.java

domain/pin/exception/
├── PinErrorCode.java
└── PinException.java
```

응답 `code` 문자열은 대문자와 언더스코어를 사용하며 다음 형식을 따릅니다.

| 구분 | 형식 | 예시 |
|------|------|------|
| 성공 코드 | `{DOMAIN}_{USE_CASE}_SUCCESS` | `MEMBER_LOGIN_SUCCESS` |
| 도메인 에러 | `{DOMAIN}_{FAILURE_REASON}` | `PIN_NOT_FOUND` |
| 공통 에러 | `COMMON_{HTTP_STATUS}_{REASON}` | `COMMON_400_BAD_REQUEST` |

HTTP 상태 코드는 공통 에러에만 포함합니다. 여러 도메인에서 같은 방식으로 처리하는 오류는 공통 에러로 정의합니다.

```java
MEMBER_LOGIN_SUCCESS
PIN_NOT_FOUND
PIN_INVALID_PIN_OWNER
COMMON_400_INVALID_CURSOR
```

도메인 예외는 공통 `BusinessException`을 상속하고 `BaseErrorCode`를 전달합니다. 도메인 고유 오류는 해당 도메인의 ErrorCode를, 여러 도메인이 공통 처리하는 오류는 `GeneralErrorCode`를 사용합니다.

```java
throw new PinException(PinErrorCode.PIN_NOT_FOUND);
```

예외 응답은 `GlobalExceptionHandler`에서 처리합니다.

Bean Validation 메시지는 한글로 작성하며, 검증 실패 시 첫 번째 필드 오류 메시지를 공통 응답에 사용합니다.

Controller에서 try-catch로 비즈니스 예외를 직접 처리하지 않습니다.

외부 API 예외는 내부 예외로 변환해서 던집니다.

```java
throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
```

## SQL Rules

테이블명과 컬럼명은 `snake_case`를 사용합니다.

```sql
pin
member_id
created_at
updated_at
```

PK, FK, Index 이름은 명시적으로 작성합니다.

| 대상 | 이름 형식 | 예시 |
|------|-----------|------|
| Primary Key | `pk_{table}` | `pk_pin` |
| Foreign Key | `fk_{from_table}_{to_table}` | `fk_pin_member` |
| Index | `idx_{table}_{column}` | `idx_pin_member_id` |
| Unique Index | `uk_{table}_{column}` | `uk_member_email` |

Boolean 컬럼은 DB에서 `is_` 접두어를 사용할 수 있습니다.

```sql
is_deleted
is_public
```

Java 필드는 getter 혼란을 피하기 위해 의미 중심으로 작성합니다.

```java
private boolean deleted;
private boolean publicVisible;
```

Enum은 문자열로 저장합니다.

```sql
status varchar(50)
```

## Test Rules

테스트 메서드명은 한글 문장형을 사용합니다.

```java
@Test
void 핀_생성에_성공한다() {
}

@Test
void 존재하지_않는_핀_조회시_예외가_발생한다() {
}
```

테스트는 Given-When-Then 구조로 작성합니다.

```java
@Test
void 핀_생성에_성공한다() {
    // given
    PinRequest.Create request = new PinRequest.Create(placeId, trackId, "content");

    // when
    PinResponse.Detail result = pinCommandService.createPin(request);

    // then
    assertThat(result.content()).isEqualTo("content");
}
```

PR의 최소 테스트 기준은 다음과 같습니다.

| 변경 내용 | 테스트 기준 |
|-----------|-------------|
| 비즈니스 로직 변경 | Service 단위 테스트 작성 |
| QueryDSL 또는 복잡한 Repository 변경 | Repository 통합 테스트 권장 |
| Controller 요청/응답 변경 | WebMvcTest 권장 |
| 문서, 패키지 구조, 단순 설정 변경 | 테스트 생략 가능 |
