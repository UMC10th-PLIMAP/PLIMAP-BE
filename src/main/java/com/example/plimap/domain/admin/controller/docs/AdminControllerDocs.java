package com.example.plimap.domain.admin.controller.docs;

import com.example.plimap.domain.admin.dto.request.AdminReqDTO;
import com.example.plimap.domain.admin.dto.response.AdminResponse;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.pin.enums.PinReportFilter;
import com.example.plimap.global.apiPayload.ApiResponse;
import com.example.plimap.global.swagger.CommonSwaggerErrorExamples;
import com.example.plimap.global.swagger.ErrorApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin", description = "관리자 API")
@ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증 정보가 없거나 유효하지 않은 경우",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorApiResponse.class),
                        examples = @ExampleObject(
                                name = "COMMON_401_UNAUTHORIZED",
                                summary = "인증 필요",
                                value = CommonSwaggerErrorExamples.UNAUTHORIZED
                        )
                )),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "ADMIN 권한이 없는 경우",
                content = @Content(
                        mediaType = "application/json",
                        schema = @Schema(implementation = ErrorApiResponse.class),
                        examples = @ExampleObject(
                                name = "COMMON_403_FORBIDDEN",
                                summary = "관리자 권한 필요",
                                value = CommonSwaggerErrorExamples.FORBIDDEN
                        )
                ))
})
public interface AdminControllerDocs {

    @Operation(
            summary = "관리자 로그인 게이트 확인",
            description = """
                    현재 로그인한 계정이 관리자(ADMIN) 권한을 가지고 있는지 확인합니다.

                    `/api/v1/admin/**` 경로는 SecurityConfig에서 ADMIN 권한이 없으면 403으로 차단되므로,
                    이 API가 200으로 응답하면 관리자 페이지 접근을 허용해도 됩니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<AdminResponse.Me> getMe(AuthMember authMember);

    @Operation(
            summary = "PIN 신고 반려",
            description = """
                    관리자가 PIN 신고를 반려합니다(grantPenalty는 false로 고정, true는 400으로 거부됩니다).
                    해당 PIN의 신고 누적 카운트(reportCount)만 0으로 초기화합니다.

                    벌점을 부여하려면 이 API 대신 최종 제재 API(POST /pins/{pinId}/sanctions)를 사용해 주세요.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "요청 값 검증에 실패했거나 grantPenalty를 true로 요청한 경우",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "COMMON_400_VALIDATION_FAILED",
                                    summary = "요청 값 검증 실패",
                                    value = CommonSwaggerErrorExamples.VALIDATION_FAILED
                            ),
                            @ExampleObject(
                                    name = "COMMON_400_MALFORMED_JSON",
                                    summary = "잘못된 JSON 본문",
                                    value = CommonSwaggerErrorExamples.MALFORMED_JSON
                            ),
                            @ExampleObject(
                                    name = "ADMIN_PENALTY_GRANT_NOT_SUPPORTED",
                                    summary = "반려 API에서 벌점 부여 요청",
                                    value = AdminSwaggerErrorExamples.PENALTY_GRANT_NOT_SUPPORTED
                            )
                    }
            ))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<Void> reviewPinReport(
            @PathVariable Long pinId,
            @RequestBody @Valid AdminReqDTO.PenaltyDecision request
    );

    @Operation(
            summary = "프로필 신고 반려",
            description = """
                    관리자가 프로필 신고를 반려합니다(grantPenalty는 false로 고정, true는 400으로 거부됩니다).
                    해당 회원의 신고 누적 카운트(reportCount)만 0으로 초기화합니다.

                    벌점을 부여하려면 이 API 대신 최종 제재 API(POST /members/{memberId}/sanctions)를 사용해 주세요.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "요청 값 검증에 실패했거나 grantPenalty를 true로 요청한 경우",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "COMMON_400_VALIDATION_FAILED",
                                    summary = "요청 값 검증 실패",
                                    value = CommonSwaggerErrorExamples.VALIDATION_FAILED
                            ),
                            @ExampleObject(
                                    name = "COMMON_400_MALFORMED_JSON",
                                    summary = "잘못된 JSON 본문",
                                    value = CommonSwaggerErrorExamples.MALFORMED_JSON
                            ),
                            @ExampleObject(
                                    name = "ADMIN_PENALTY_GRANT_NOT_SUPPORTED",
                                    summary = "반려 API에서 벌점 부여 요청",
                                    value = AdminSwaggerErrorExamples.PENALTY_GRANT_NOT_SUPPORTED
                            )
                    }
            ))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<Void> reviewProfileReport(
            @PathVariable Long memberId,
            @RequestBody @Valid AdminReqDTO.PenaltyDecision request
    );

    @Operation(
            summary = "PIN 신고 최종 제재(기간·사유 확정)",
            description = """
                    관리자가 해당 PIN에 걸린 기존 신고 중 하나(reportId)를 지목해 제재 사유로 확정하고,
                    제재 기간(period)을 선택해 최종 제재를 부여합니다.

                    - PIN을 soft delete 처리하고, 지목한 신고의 category/detail을 작성자의 최종 제재 사유로 스냅샷 저장합니다.
                    - period=ONE_DAY/THREE_DAYS/FIVE_DAYS: 해당 기간만큼 정지(SUSPENDED)로 전환합니다.
                    - period=PERMANENT: 누적 벌점(penaltyPoint)과 무관하게 즉시 영구 탈퇴 처리합니다.
                    - reportId가 이 PIN에 대한 신고가 아니면 400으로 거부됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 값 검증에 실패했거나 신고와 PIN이 일치하지 않는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "COMMON_400_VALIDATION_FAILED",
                                            summary = "요청 값 검증 실패",
                                            value = CommonSwaggerErrorExamples.VALIDATION_FAILED
                                    ),
                                    @ExampleObject(
                                            name = "COMMON_400_MALFORMED_JSON",
                                            summary = "잘못된 JSON 본문",
                                            value = CommonSwaggerErrorExamples.MALFORMED_JSON
                                    ),
                                    @ExampleObject(
                                            name = "REPORT_PIN_MISMATCH",
                                            summary = "신고 대상 PIN 불일치",
                                            value = AdminSwaggerErrorExamples.REPORT_PIN_MISMATCH
                                    )
                            }
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "신고, PIN 또는 제재 대상 회원을 찾을 수 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "REPORT_NOT_FOUND",
                                            summary = "신고 없음",
                                            value = AdminSwaggerErrorExamples.REPORT_NOT_FOUND
                                    ),
                                    @ExampleObject(
                                            name = "PIN_NOT_FOUND",
                                            summary = "PIN 없음",
                                            value = AdminSwaggerErrorExamples.PIN_NOT_FOUND
                                    ),
                                    @ExampleObject(
                                            name = "MEMBER_NOT_FOUND",
                                            summary = "회원 없음",
                                            value = AdminSwaggerErrorExamples.MEMBER_NOT_FOUND
                                    )
                            }
                    ))
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<Void> grantPinSanction(
            @PathVariable Long pinId,
            @RequestBody @Valid AdminReqDTO.PinSanctionDecision request
    );

    @Operation(
            summary = "프로필 신고 최종 제재(기간·사유 확정)",
            description = """
                    관리자가 제재 사유(category, OTHER인 경우 detail 필수)를 직접 작성하고,
                    제재 기간(period)을 선택해 최종 제재를 부여합니다. 기존 신고함에서 고르지 않습니다.

                    - 닉네임을 후보 풀에서 미사용 값으로 강제 치환하고, 작성한 사유를 최종 제재 사유로 스냅샷 저장합니다.
                    - period=ONE_DAY/THREE_DAYS/FIVE_DAYS: 해당 기간만큼 정지(SUSPENDED)로 전환합니다.
                    - period=PERMANENT: 누적 벌점(penaltyPoint)과 무관하게 즉시 영구 탈퇴 처리합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "제재 기간·카테고리·상세 내용 검증에 실패한 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = {
                                    @ExampleObject(
                                            name = "COMMON_400_VALIDATION_FAILED",
                                            summary = "요청 값 검증 실패",
                                            value = CommonSwaggerErrorExamples.VALIDATION_FAILED
                                    ),
                                    @ExampleObject(
                                            name = "COMMON_400_MALFORMED_JSON",
                                            summary = "잘못된 JSON 본문",
                                            value = CommonSwaggerErrorExamples.MALFORMED_JSON
                                    )
                            }
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "제재 대상 회원을 찾을 수 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "MEMBER_NOT_FOUND",
                                    summary = "회원 없음",
                                    value = AdminSwaggerErrorExamples.MEMBER_NOT_FOUND
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "제재용 닉네임 후보를 찾지 못한 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "MEMBER_PENALTY_NICKNAME_POOL_EXHAUSTED",
                                    summary = "제재용 닉네임 후보 고갈",
                                    value = AdminSwaggerErrorExamples.PENALTY_NICKNAME_POOL_EXHAUSTED
                            )
                    ))
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<Void> grantMemberSanction(
            @PathVariable Long memberId,
            @RequestBody @Valid AdminReqDTO.MemberSanctionDecision request
    );

    @Operation(
            summary = "신고 누적 게시물 목록 조회",
            description = """
                    신고가 1건 이상 누적된 PIN을 신고 누적 수(reportCount) 내림차순으로 조회합니다.
                    각 항목에는 신고 사유(신고ID/카테고리/상세/신고자/신고일) 목록이 함께 포함됩니다.
                    이 중 reportId는 최종 제재 API(POST /pins/{pinId}/sanctions)에서 제재 사유로 지목할 신고를 고를 때 사용합니다.

                    - filter=ALL: 신고가 1건 이상인 모든 PIN
                    - filter=AUTO_HIDDEN: 신고 누적 수가 10회 이상이라 피드에서 자동숨김된 PIN
                    - filter=BELOW_THRESHOLD: 신고 누적 수가 10회 미만인 PIN
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "필터·페이지 요청 값의 타입 또는 범위가 올바르지 않은 경우",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "COMMON_400_VALIDATION_FAILED",
                                    summary = "페이지 범위 검증 실패",
                                    value = CommonSwaggerErrorExamples.VALIDATION_FAILED
                            ),
                            @ExampleObject(
                                    name = "COMMON_400_TYPE_MISMATCH",
                                    summary = "필터 또는 페이지 타입 오류",
                                    value = CommonSwaggerErrorExamples.TYPE_MISMATCH
                            )
                    }
            ))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<AdminResponse.ReportedPinPage> getReportedPins(
            @RequestParam(defaultValue = "ALL") PinReportFilter filter,
            @Min(1) @RequestParam(defaultValue = "1") Integer page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "10") Integer pageSize
    );

    @Operation(
            summary = "회원 검색/목록 조회",
            description = """
                    닉네임/이름/이메일로 회원을 검색하고, 상태(status)로 필터링해 조회합니다.
                    query와 status는 모두 선택값이며, 지정하지 않으면 전체 회원을 대상으로 합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "회원 상태·페이지 요청 값의 타입 또는 범위가 올바르지 않은 경우",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "COMMON_400_VALIDATION_FAILED",
                                    summary = "페이지 범위 검증 실패",
                                    value = CommonSwaggerErrorExamples.VALIDATION_FAILED
                            ),
                            @ExampleObject(
                                    name = "COMMON_400_TYPE_MISMATCH",
                                    summary = "회원 상태 또는 페이지 타입 오류",
                                    value = CommonSwaggerErrorExamples.TYPE_MISMATCH
                            )
                    }
            ))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<AdminResponse.MemberPage> getMembers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MemberStatus status,
            @Min(1) @RequestParam(defaultValue = "1") Integer page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "10") Integer pageSize
    );

    @Operation(
            summary = "회원 상세 조회",
            description = "경로의 memberId에 해당하는 회원의 상세 정보(상태, 역할, 벌점, 정지 해제일, 탈퇴 사유 등)를 조회합니다. 탈퇴/정지 회원도 조회할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "회원을 찾을 수 없는 경우",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples = @ExampleObject(
                            name = "MEMBER_NOT_FOUND",
                            summary = "회원 없음",
                            value = AdminSwaggerErrorExamples.MEMBER_NOT_FOUND
                    )
            ))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<AdminResponse.MemberDetail> getMemberDetail(@PathVariable Long memberId);

    @Operation(
            summary = "회원 닉네임 강제 재생성(벌점 없이)",
            description = """
                    벌점 부여 없이 회원의 닉네임만 후보 풀에서 미사용 값으로 강제 치환합니다.
                    신고 검토(POST /members/{memberId}/penalty)와 달리 penaltyPoint와 reportCount는 변경되지 않습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "회원을 찾을 수 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "MEMBER_NOT_FOUND",
                                    summary = "회원 없음",
                                    value = AdminSwaggerErrorExamples.MEMBER_NOT_FOUND
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "재생성할 닉네임 후보를 찾지 못한 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "MEMBER_PENALTY_NICKNAME_POOL_EXHAUSTED",
                                    summary = "닉네임 후보 고갈",
                                    value = AdminSwaggerErrorExamples.PENALTY_NICKNAME_POOL_EXHAUSTED
                            )
                    ))
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<AdminResponse.MemberDetail> regenerateMemberNickname(@PathVariable Long memberId);

    @Operation(
            summary = "문의 목록 조회",
            description = """
                    접수된 문의를 최신순으로 커서 기반 페이지네이션으로 조회합니다.
                    category는 선택값이며, 지정하지 않으면 전체 카테고리를 대상으로 합니다.

                    첫 페이지는 cursor 없이 요청하고, 이후에는 응답의 nextCursor를 그대로 다음 요청의 cursor로 전달합니다. pageSize는 1~100 사이여야 하며 기본값은 10입니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "카테고리·페이지 요청 값 또는 커서가 올바르지 않은 경우",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples = {
                            @ExampleObject(
                                    name = "COMMON_400_VALIDATION_FAILED",
                                    summary = "페이지 범위 검증 실패",
                                    value = CommonSwaggerErrorExamples.VALIDATION_FAILED
                            ),
                            @ExampleObject(
                                    name = "COMMON_400_TYPE_MISMATCH",
                                    summary = "카테고리 타입 오류",
                                    value = CommonSwaggerErrorExamples.TYPE_MISMATCH
                            ),
                            @ExampleObject(
                                    name = "COMMON_400_INVALID_CURSOR",
                                    summary = "잘못된 커서",
                                    value = CommonSwaggerErrorExamples.INVALID_CURSOR
                            )
                    }
            ))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<AdminResponse.InquiryPage> getInquiries(
            @RequestParam(required = false) InquiryCategory category,
            String cursor,
            @Min(1) @Max(100) @RequestParam(defaultValue = "10") Integer pageSize
    );

    @Operation(
            summary = "문의 상세 조회",
            description = "경로의 inquiryId에 해당하는 문의의 상세 정보(제목, 내용, 작성자, 연락 이메일 등)를 조회합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "문의를 찾을 수 없는 경우",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ErrorApiResponse.class),
                    examples = @ExampleObject(
                            name = "INQUIRY_NOT_FOUND",
                            summary = "문의 없음",
                            value = AdminSwaggerErrorExamples.INQUIRY_NOT_FOUND
                    )
            ))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "요청 성공")
    ApiResponse<AdminResponse.InquiryDetail> getInquiryDetail(@PathVariable Long inquiryId);
}
