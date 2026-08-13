package com.example.plimap.domain.auth.controller.docs;

import com.example.plimap.domain.auth.dto.response.AuthResponse;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.request.TermsReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResponse;
import com.example.plimap.domain.member.dto.response.TermsResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.web.csrf.CsrfToken;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "CSRF 토큰 발급",
            description = "쿠키 인증 상태 변경 요청의 X-XSRF-TOKEN 헤더에 사용할 CSRF 토큰을 반환합니다."
    )
    ApiResponse<AuthResponse.CsrfToken> getCsrfToken(
            @Parameter(hidden = true) CsrfToken csrfToken
    );

    @Operation(
            summary = "온보딩 (닉네임/프로필 설정)",
            description = "최초 가입 후 닉네임과 프로필 정보를 등록하여 온보딩을 완료합니다."
    )
    ApiResponse<MemberResponse.Onboarding> onboarding(AuthMember authMember, MemberReqDTO.Onboarding request);

    @Operation(
            summary = "약관 동의 여부 조회",
            description = """
                    현재 활성화된 약관 유형별로, 로그인한 회원이 동의했는지 여부와 동의 일시를 조회합니다.

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
                      "code": "TERMS_AGREEMENT_STATUS_RETRIEVED_SUCCESS",
                      "message": "약관 동의 여부를 조회했습니다.",
                      "result": [
                        { "type": "SERVICE", "agreed": true, "agreedAt": "2026-07-13T07:19:16.301Z" },
                        { "type": "PRIVACY", "agreed": true, "agreedAt": "2026-07-13T07:19:16.301Z" },
                        { "type": "LOCATION", "agreed": false, "agreedAt": null },
                        { "type": "MARKETING", "agreed": false, "agreedAt": null }
                      ]
                    }
                    """))
    )
    ApiResponse<List<TermsResponse.Result>> getTermsAgreementStatus(AuthMember authMember);

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
                      "code": "TERMS_AGREED_SUCCESS",
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
    ApiResponse<List<TermsResponse.Result>> agreeToTerms(AuthMember authMember, TermsReqDTO.Agree request);

    @Operation(
            summary = "로그아웃",
            description = "현재 액세스 토큰을 서버 측에서 무효화(블랙리스트 등록)하고, 저장된 리프레시 토큰과 accessToken/refreshToken 쿠키를 삭제합니다."
    )
    ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response);

    @Operation(
            summary = "토큰 재발급",
            description = "refreshToken 쿠키를 검증하여 새로운 Access/Refresh Token을 발급하고 쿠키를 갱신합니다(Refresh Token Rotation)."
    )
    ApiResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response);
}
