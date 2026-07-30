package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.exception.AuthErrorCode;
import com.example.plimap.domain.auth.exception.AuthException;
import com.example.plimap.domain.auth.service.command.TestTokenCommandService;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.SwaggerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthTestControllerTest {

    private static final String ISSUE_KEY = "test-only-issue-key!".repeat(2);

    private final TestTokenCommandService testTokenCommandService =
            mock(TestTokenCommandService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AuthTestController(testTokenCommandService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void 발급_키와_회원_ID를_전달해_테스트_토큰을_발급한다() throws Exception {
        when(testTokenCommandService.issueTestToken(1L, ISSUE_KEY))
                .thenReturn("test-access-token");

        mockMvc.perform(post("/api/v1/auth/token/test")
                        .header(SwaggerConfig.TEST_TOKEN_ISSUE_KEY_HEADER, ISSUE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.accessToken").value("test-access-token"));
    }

    @Test
    void Local_호환을_위해_발급_키_헤더는_바인딩_단계에서_선택값이다() throws Exception {
        when(testTokenCommandService.issueTestToken(1L, null))
                .thenReturn("local-test-access-token");

        mockMvc.perform(post("/api/v1/auth/token/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accessToken").value("local-test-access-token"));
    }

    @Test
    void 발급_키가_누락되어도_키_불일치와_동일한_401_응답을_반환한다() throws Exception {
        when(testTokenCommandService.issueTestToken(1L, null))
                .thenThrow(new AuthException(AuthErrorCode.TEST_TOKEN_ISSUE_UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/token/test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 1
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_401_TEST_TOKEN_ISSUE_UNAUTHORIZED"));
    }

    @Test
    void 발급_키_검증에_실패하면_동일한_401_응답을_반환한다() throws Exception {
        when(testTokenCommandService.issueTestToken(1L, "wrong-key"))
                .thenThrow(new AuthException(AuthErrorCode.TEST_TOKEN_ISSUE_UNAUTHORIZED));

        mockMvc.perform(post("/api/v1/auth/token/test")
                        .header(SwaggerConfig.TEST_TOKEN_ISSUE_KEY_HEADER, "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "memberId": 1
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_401_TEST_TOKEN_ISSUE_UNAUTHORIZED"));
    }
}
