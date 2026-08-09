package com.example.plimap.domain.inquiry.controller;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.inquiry.dto.request.InquiryRequest;
import com.example.plimap.domain.inquiry.service.command.InquiryCommandService;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
import com.example.plimap.global.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.SecurityErrorResponseHandler;
import com.example.plimap.global.security.TokenBlacklistService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InquiryController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class InquiryControllerTest {

    private static final String ENDPOINT = "/api/v1/inquiries";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CookieCsrfTokenRepository csrfTokenRepository;

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
    private InquiryCommandService inquiryCommandService;

    @BeforeEach
    void setUp() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(ACCESS_TOKEN)).thenReturn("test-jti");
        when(tokenBlacklistService.isBlacklisted("test-jti")).thenReturn(false);
        when(jwtUtil.getMemberId(ACCESS_TOKEN)).thenReturn(1L);
    }

    @Test
    void 로그인_사용자가_문의를_등록하면_201을_반환한다() throws Exception {
        Member member = Member.builder().nickname("작성자").status(MemberStatus.ACTIVE).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        mockMvc.perform(authenticatedPost(ENDPOINT, validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("INQUIRY_CREATE_SUCCESS"))
                .andExpect(jsonPath("$.message").value("문의가 접수되었습니다."))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(inquiryCommandService).createInquiry(eq(member), any(InquiryRequest.Create.class));
    }

    @Test
    void 탈퇴_회원은_작성자로_연결되지_않는다() throws Exception {
        Member withdrawnMember = Member.builder().nickname("탈퇴회원").status(MemberStatus.WITHDRAWN).build();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(withdrawnMember));

        mockMvc.perform(authenticatedPost(ENDPOINT, validRequest()))
                .andExpect(status().isCreated());

        verify(inquiryCommandService).createInquiry(isNull(), any(InquiryRequest.Create.class));
    }

    @Test
    void 비로그인_사용자도_CSRF_토큰만_있으면_문의를_등록할_수_있다() throws Exception {
        mockMvc.perform(withCsrf(post(ENDPOINT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("INQUIRY_CREATE_SUCCESS"));

        verify(inquiryCommandService).createInquiry(isNull(), any(InquiryRequest.Create.class));
    }

    @Test
    void 카테고리가_누락되면_400을_반환한다() throws Exception {
        mockMvc.perform(withCsrf(post(ENDPOINT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "제목",
                                  "content": "내용",
                                  "contactEmail": "user@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("문의 카테고리를 입력해주세요."));
    }

    @Test
    void 이메일_형식이_올바르지_않으면_400을_반환한다() throws Exception {
        mockMvc.perform(withCsrf(post(ENDPOINT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "OTHER",
                                  "title": "제목",
                                  "content": "내용",
                                  "contactEmail": "invalid-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다."));
    }

    @Test
    void 이메일이_320자를_초과하면_400을_반환한다() throws Exception {
        // DB contact_email 컬럼은 VARCHAR(320)이라, 형식이 맞더라도 320자를 넘으면 저장 단계(500)가 아니라
        // 요청 검증 단계(400)에서 걸러져야 한다. Hibernate Validator의 @Email 자체가 320자보다 긴
        // 유효한 형식의 문자열을 만들지 못하므로(local 64자 + '@' + domain 255자 제한), 형식이 깨진
        // 321자 문자열로도 여전히 400이 나가는지만 확인한다.
        String tooLongEmail = "a".repeat(321);

        mockMvc.perform(withCsrf(post(ENDPOINT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "OTHER",
                                  "title": "제목",
                                  "content": "내용",
                                  "contactEmail": "%s"
                                }
                                """.formatted(tooLongEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"));
    }

    private MockHttpServletRequestBuilder authenticatedPost(String endpoint, String content) {
        return post(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder builder) {
        MockHttpServletRequest tokenRequest = new MockHttpServletRequest();
        MockHttpServletResponse tokenResponse = new MockHttpServletResponse();
        CsrfToken token = csrfTokenRepository.generateToken(tokenRequest);
        csrfTokenRepository.saveToken(token, tokenRequest, tokenResponse);
        Cookie cookie = tokenResponse.getCookie("XSRF-TOKEN");
        return builder.cookie(cookie).header(token.getHeaderName(), token.getToken());
    }

    private String validRequest() {
        return """
                {
                  "category": "APP_BUG_OR_ERROR",
                  "title": "핀 재생이 안 돼요",
                  "content": "특정 곡의 PIN이 재생되지 않습니다.",
                  "contactEmail": "user@example.com"
                }
                """;
    }
}
