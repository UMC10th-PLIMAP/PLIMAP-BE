package com.example.plimap.global.security;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityIntegrationTest.TestController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        SecurityIntegrationTest.TestController.class
})
@ActiveProfiles("test")
class SecurityIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String PROTECTED_PATH = "/api/v1/security-test";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomOAuthService customOAuthService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        Member member = Member.builder().build();
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getMemberId(ACCESS_TOKEN)).thenReturn(1L);
        when(jwtUtil.getJti(ACCESS_TOKEN)).thenReturn("test-jti");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(tokenBlacklistService.isBlacklisted("test-jti")).thenReturn(false);
    }

    @Test
    void 미인증_요청은_공통_401_응답을_반환한다() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_401_UNAUTHORIZED"));
    }

    @Test
    void OAuth와_Swagger_OpenAPI_경로는_인증_없이_접근할_수_있다() throws Exception {
        mockMvc.perform(get("/oauth/authorization/kakao"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }

    @Test
    void 최초_GET_응답에서_읽을_수_있는_CSRF_쿠키를_발급한다() throws Exception {
        MvcResult result = mockMvc.perform(get(PROTECTED_PATH)
                        .cookie(new Cookie("accessToken", ACCESS_TOKEN)))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"))
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        assertThat(csrfCookie.isHttpOnly()).isFalse();
        assertThat(csrfCookie.getSecure()).isFalse();
        assertThat(csrfCookie.getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(csrfCookie.getPath()).isEqualTo("/");
    }

    @Test
    void 쿠키_인증_POST는_CSRF_토큰이_없으면_차단한다() throws Exception {
        mockMvc.perform(post(PROTECTED_PATH)
                        .cookie(new Cookie("accessToken", ACCESS_TOKEN)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_403_FORBIDDEN"));
    }

    @Test
    void 쿠키와_헤더의_CSRF_토큰이_일치하면_POST가_CSRF_검사를_통과한다() throws Exception {
        Cookie csrfCookie = issueCsrfCookie();

        mockMvc.perform(post(PROTECTED_PATH)
                        .cookie(new Cookie("accessToken", ACCESS_TOKEN), csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void Bearer_인증_POST는_CSRF_토큰_없이_CSRF_검사를_통과한다() throws Exception {
        mockMvc.perform(post(PROTECTED_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void 유효하지_않은_Bearer가_있으면_쿠키_인증으로_fallback하지_않는다() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .cookie(new Cookie("accessToken", ACCESS_TOKEN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401_UNAUTHORIZED"));
    }

    @Test
    void 허용된_Origin의_credential_Preflight를_처리한다() throws Exception {
        mockMvc.perform(options(PROTECTED_PATH)
                        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                "Authorization, Content-Type, X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        containsString("X-XSRF-TOKEN")
                ));
    }

    @Test
    void 허용되지_않은_Origin의_Preflight를_차단한다() throws Exception {
        mockMvc.perform(options(PROTECTED_PATH)
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private Cookie issueCsrfCookie() throws Exception {
        MvcResult result = mockMvc.perform(get(PROTECTED_PATH)
                        .cookie(new Cookie("accessToken", ACCESS_TOKEN)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        return csrfCookie;
    }

    @RestController
    @RequestMapping(PROTECTED_PATH)
    public static class TestController {

        @GetMapping
        public ResponseEntity<String> get() {
            return ResponseEntity.ok("ok");
        }

        @PostMapping
        public ResponseEntity<String> post() {
            return ResponseEntity.ok("ok");
        }
    }
}
