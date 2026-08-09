package com.example.plimap.domain.report.controller;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.report.dto.request.ReportRequest;
import com.example.plimap.domain.report.exception.ReportErrorCode;
import com.example.plimap.domain.report.exception.ReportException;
import com.example.plimap.domain.report.service.command.ReportCommandService;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
import com.example.plimap.global.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.SecurityErrorResponseHandler;
import com.example.plimap.global.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReportController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class ReportControllerTest {

    private static final String MEMBER_REPORT_ENDPOINT = "/api/v1/reports/members/2";
    private static final String PIN_REPORT_ENDPOINT = "/api/v1/reports/pins/2";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomOAuthService customOAuthService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean
    private OAuthFailureHandler oAuthFailureHandler;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private MemberCommandService memberCommandService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @MockitoBean
    private ReportCommandService reportCommandService;

    @BeforeEach
    void setUp() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(ACCESS_TOKEN)).thenReturn("test-jti");
        when(tokenBlacklistService.isBlacklisted("test-jti")).thenReturn(false);
        when(jwtUtil.getMemberId(ACCESS_TOKEN)).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(Member.builder().nickname("신고자").build()));
    }

    @Test
    void 회원_신고에_성공하면_201을_반환한다() throws Exception {
        mockMvc.perform(authenticatedPost(MEMBER_REPORT_ENDPOINT, normalCategoryRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("REPORT_CREATE_SUCCESS"))
                .andExpect(jsonPath("$.message").value("신고가 접수되었습니다."))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(reportCommandService).reportMember(
                any(Member.class),
                eq(2L),
                any(ReportRequest.Create.class)
        );
    }

    @Test
    void PIN_신고에_성공하면_201을_반환한다() throws Exception {
        mockMvc.perform(authenticatedPost(PIN_REPORT_ENDPOINT, normalCategoryRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("REPORT_CREATE_SUCCESS"))
                .andExpect(jsonPath("$.message").value("신고가 접수되었습니다."))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(reportCommandService).reportPin(
                any(Member.class),
                eq(2L),
                any(ReportRequest.Create.class)
        );
    }

    @Test
    void 기타_내용을_한_글자로_입력해도_201을_반환한다() throws Exception {
        mockMvc.perform(authenticatedPost(MEMBER_REPORT_ENDPOINT, otherCategoryRequest("가")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("REPORT_CREATE_SUCCESS"));
    }

    @Test
    void 신고_카테고리가_누락되면_400을_반환한다() throws Exception {
        mockMvc.perform(authenticatedPost(MEMBER_REPORT_ENDPOINT, """
                        {
                          "detail": null
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("신고 카테고리를 입력해주세요."));

        verifyNoInteractions(reportCommandService);
    }

    @Test
    void 기타_내용이_누락되면_400을_반환한다() throws Exception {
        assertInvalidDetail(otherCategoryRequest(null));
    }

    @Test
    void 기타_내용이_빈_문자열이면_400을_반환한다() throws Exception {
        assertInvalidDetail(otherCategoryRequest(""));
    }

    @Test
    void 기타_내용이_공백이면_400을_반환한다() throws Exception {
        assertInvalidDetail(otherCategoryRequest(" "));
    }

    @Test
    void 일반_카테고리에_상세_내용을_전달하면_400을_반환한다() throws Exception {
        assertInvalidDetail("""
                {
                  "category": "OBSCENE_OR_HARMFUL",
                  "detail": "허용되지 않는 내용"
                }
                """);
    }

    @Test
    void 중복_회원_신고는_409를_반환한다() throws Exception {
        doThrow(new ReportException(ReportErrorCode.REPORT_MEMBER_ALREADY_EXISTS))
                .when(reportCommandService)
                .reportMember(any(Member.class), eq(2L), any(ReportRequest.Create.class));

        mockMvc.perform(authenticatedPost(MEMBER_REPORT_ENDPOINT, normalCategoryRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_MEMBER_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 신고한 회원입니다."));
    }

    @Test
    void 인증과_CSRF_정보가_없으면_403을_반환한다() throws Exception {
        mockMvc.perform(post(MEMBER_REPORT_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(normalCategoryRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_403_FORBIDDEN"));

        verifyNoInteractions(reportCommandService);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedPost(
            String endpoint,
            String content
    ) {
        return post(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }

    private void assertInvalidDetail(String content) throws Exception {
        mockMvc.perform(authenticatedPost(MEMBER_REPORT_ENDPOINT, content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("신고 상세 내용이 카테고리 조건에 맞지 않습니다."));

        verifyNoInteractions(reportCommandService);
    }

    private String normalCategoryRequest() {
        return """
                {
                  "category": "OBSCENE_OR_HARMFUL",
                  "detail": null
                }
                """;
    }

    private String otherCategoryRequest(String detail) {
        String detailJson = detail == null ? "null" : "\"" + detail + "\"";
        return """
                {
                  "category": "OTHER",
                  "detail": %s
                }
                """.formatted(detailJson);
    }
}