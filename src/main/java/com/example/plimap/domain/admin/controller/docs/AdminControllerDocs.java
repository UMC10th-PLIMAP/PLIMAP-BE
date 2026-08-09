package com.example.plimap.domain.admin.controller.docs;

import com.example.plimap.domain.admin.dto.request.AdminReqDTO;
import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.pin.enums.PinReportFilter;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Admin", description = "관리자 API")
public interface AdminControllerDocs {

    @Operation(
            summary = "관리자 로그인 게이트 확인",
            description = """
                    현재 로그인한 계정이 관리자(ADMIN) 권한을 가지고 있는지 확인합니다.

                    `/api/v1/admin/**` 경로는 SecurityConfig에서 ADMIN 권한이 없으면 403으로 차단되므로,
                    이 API가 200으로 응답하면 관리자 페이지 접근을 허용해도 됩니다.
                    """
    )
    ApiResponse<AdminResDTO.Me> getMe(AuthMember authMember);

    @Operation(
            summary = "PIN 신고 검토(벌점 부여/미부여)",
            description = """
                    신고 접수 여부와 무관하게 관리자가 PIN을 직접 검토해 벌점 부여 여부를 결정합니다.

                    - grantPenalty=true: PIN을 soft delete 처리하고 작성자의 penaltyPoint를 1점 올립니다.
                      penaltyPoint가 1~3점이면 정지(1/3/5일)로, 4점이면 자동 탈퇴로 전환됩니다.
                    - grantPenalty=false: 해당 PIN의 신고 누적 카운트(reportCount)만 0으로 초기화합니다.
                    """
    )
    ApiResponse<Void> reviewPinReport(
            @PathVariable Long pinId,
            @RequestBody @Valid AdminReqDTO.PenaltyDecision request
    );

    @Operation(
            summary = "프로필 신고 검토(벌점 부여/미부여)",
            description = """
                    신고 접수 여부와 무관하게 관리자가 회원 프로필(닉네임)을 직접 검토해 벌점 부여 여부를 결정합니다.

                    - grantPenalty=true: 닉네임을 후보 풀에서 미사용 값으로 강제 치환하고 penaltyPoint를 1점 올립니다.
                      penaltyPoint가 1~3점이면 정지(1/3/5일)로, 4점이면 자동 탈퇴로 전환됩니다.
                    - grantPenalty=false: 해당 회원의 신고 누적 카운트(reportCount)만 0으로 초기화합니다.
                    """
    )
    ApiResponse<Void> reviewProfileReport(
            @PathVariable Long memberId,
            @RequestBody @Valid AdminReqDTO.PenaltyDecision request
    );

    @Operation(
            summary = "신고 누적 게시물 목록 조회",
            description = """
                    신고가 1건 이상 누적된 PIN을 신고 누적 수(reportCount) 내림차순으로 조회합니다.
                    각 항목에는 신고 사유(카테고리/상세/신고자/신고일) 목록이 함께 포함됩니다.

                    - filter=ALL: 신고가 1건 이상인 모든 PIN
                    - filter=AUTO_HIDDEN: 신고 누적 수가 10회 이상이라 피드에서 자동숨김된 PIN
                    - filter=BELOW_THRESHOLD: 신고 누적 수가 10회 미만인 PIN
                    """
    )
    ApiResponse<AdminResDTO.ReportedPinPage> getReportedPins(
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
    ApiResponse<AdminResDTO.MemberPage> getMembers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) MemberStatus status,
            @Min(1) @RequestParam(defaultValue = "1") Integer page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "10") Integer pageSize
    );

    @Operation(
            summary = "회원 상세 조회",
            description = "경로의 memberId에 해당하는 회원의 상세 정보(상태, 역할, 벌점, 정지 해제일, 탈퇴 사유 등)를 조회합니다. 탈퇴/정지 회원도 조회할 수 있습니다."
    )
    ApiResponse<AdminResDTO.MemberDetail> getMemberDetail(@PathVariable Long memberId);

    @Operation(
            summary = "회원 닉네임 강제 재생성(벌점 없이)",
            description = """
                    벌점 부여 없이 회원의 닉네임만 후보 풀에서 미사용 값으로 강제 치환합니다.
                    신고 검토(POST /members/{memberId}/penalty)와 달리 penaltyPoint와 reportCount는 변경되지 않습니다.
                    """
    )
    ApiResponse<AdminResDTO.MemberDetail> regenerateMemberNickname(@PathVariable Long memberId);

    @Operation(
            summary = "문의 목록 조회",
            description = """
                    접수된 문의를 최신순으로 조회합니다. category는 선택값이며, 지정하지 않으면 전체 카테고리를 대상으로 합니다.
                    """
    )
    ApiResponse<AdminResDTO.InquiryPage> getInquiries(
            @RequestParam(required = false) InquiryCategory category,
            @Min(1) @RequestParam(defaultValue = "1") Integer page,
            @Min(1) @Max(100) @RequestParam(defaultValue = "10") Integer pageSize
    );

    @Operation(
            summary = "문의 상세 조회",
            description = "경로의 inquiryId에 해당하는 문의의 상세 정보(제목, 내용, 작성자, 연락 이메일 등)를 조회합니다."
    )
    ApiResponse<AdminResDTO.InquiryDetail> getInquiryDetail(@PathVariable Long inquiryId);
}
