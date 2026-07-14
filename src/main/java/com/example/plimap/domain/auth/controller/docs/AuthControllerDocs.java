package com.example.plimap.domain.auth.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.request.TermsReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.dto.response.TermsResDTO;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "온보딩 (닉네임/프로필 설정)",
            description = "최초 가입 후 닉네임과 프로필 정보를 등록하여 온보딩을 완료합니다."
    )
    ApiResponse<MemberResDTO.Onboarding> onboarding(AuthMember authMember, MemberReqDTO.Onboarding request);

    @Operation(
            summary = "활성 약관 목록 조회",
            description = """
                    현재 활성화된 약관 목록을 조회합니다.

                    **약관 유형(type)**
                    - SERVICE (필수): 플리맵 이용약관
                    - PRIVACY (필수): 개인정보 수집 및 이용 동의
                    - LOCATION (필수): 위치기반서비스 이용약관
                    - MARKETING (선택): 마케팅 정보 수신 동의
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "isSuccess": true,
                      "code": "TERMS_200_ACTIVE_TERMS_RETRIEVED",
                      "message": "활성 약관 목록을 조회했습니다.",
                      "result": [
                        { "type": "SERVICE", "version": "v1", "required": true },
                        { "type": "PRIVACY", "version": "v1", "required": true },
                        { "type": "LOCATION", "version": "v1", "required": true },
                        { "type": "MARKETING", "version": "v1", "required": false }
                      ]
                    }
                    """))
    )
    ApiResponse<List<TermsResDTO.Item>> getActiveTerms();

    @Operation(
            summary = "약관 동의",
            description = """
                    약관 유형별 동의 여부를 등록합니다. 활성 필수 약관에 모두 동의하지 않으면 실패합니다.

                    **약관 유형(type)**
                    - SERVICE (필수): 플리맵 이용약관
                    - PRIVACY (필수): 개인정보 수집 및 이용 동의
                    - LOCATION (필수): 위치기반서비스 이용약관
                    - MARKETING (선택): 마케팅 정보 수신 동의
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "isSuccess": true,
                      "code": "TERMS_200_TERMS_AGREED",
                      "message": "약관 동의가 완료되었습니다.",
                      "result": [
                        {
                          "type": "SERVICE",
                          "agreed": true,
                          "agreedAt": "2026-07-13T07:19:16.301Z"
                        }
                      ]
                    }
                    """))
    )
    ApiResponse<List<TermsResDTO.Result>> agreeToTerms(AuthMember authMember, TermsReqDTO.Agree request);

    @Operation(
            summary = "로그아웃",
            description = "현재 액세스 토큰을 서버 측에서 무효화(블랙리스트 등록)하고, accessToken 쿠키를 삭제합니다."
    )
    ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response);
}
