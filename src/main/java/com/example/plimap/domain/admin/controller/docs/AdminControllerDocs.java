package com.example.plimap.domain.admin.controller.docs;

import com.example.plimap.domain.admin.dto.request.AdminReqDTO;
import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

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
}
