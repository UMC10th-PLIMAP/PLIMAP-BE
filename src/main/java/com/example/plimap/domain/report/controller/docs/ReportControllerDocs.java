package com.example.plimap.domain.report.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.report.dto.request.ReportRequest;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface ReportControllerDocs {

    @Operation(
            summary = "회원 신고 접수",
            description = """
                    로그인한 사용자가 다른 회원을 신고합니다. (Figma 기준 화면: FD-02-01)

                    **신고 카테고리(category)**
                    - PERSONAL_INFORMATION_EXPOSURE: 개인정보 노출
                    - OBSCENE_OR_HARMFUL: 음란성 또는 유해성 콘텐츠
                    - ABUSE_OR_HATE_SPEECH: 욕설 또는 혐오 표현
                    - COMMERCIAL_OR_PROMOTIONAL: 상업성 또는 홍보성 콘텐츠
                    - OTHER: 기타 신고

                    **상세 내용(detail)**
                    - OTHER: 공백이 아닌 상세 내용 필수
                    - 그 외 카테고리: 생략하거나 null로 전송
                    """
    )
    ResponseEntity<ApiResponse<Void>> reportMember(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long memberId,
            @RequestBody @Valid ReportRequest.Create request
    );

    @Operation(
            summary = "PIN 신고 접수",
            description = """
                    로그인한 사용자가 공개 피드인 PIN을 신고합니다. (Figma 기준 화면: PN-01-03-a, b, c, d)

                    **신고 카테고리(category)**
                    - PERSONAL_INFORMATION_EXPOSURE: 개인정보 노출
                    - OBSCENE_OR_HARMFUL: 음란성 또는 유해성 콘텐츠
                    - ABUSE_OR_HATE_SPEECH: 욕설 또는 혐오 표현
                    - COMMERCIAL_OR_PROMOTIONAL: 상업성 또는 홍보성 콘텐츠
                    - OTHER: 기타 신고

                    **상세 내용(detail)**
                    - OTHER: 공백이 아닌 상세 내용 필수
                    - 그 외 카테고리: 생략하거나 null로 전송
                    """
    )
    ResponseEntity<ApiResponse<Void>> reportPin(
            @AuthenticationPrincipal AuthMember currentMember,
            @PathVariable Long pinId,
            @RequestBody @Valid ReportRequest.Create request
    );
}