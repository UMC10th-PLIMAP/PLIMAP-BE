package com.example.plimap.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorResponseHandlerTest {

    private final SecurityErrorResponseHandler handler =
            new SecurityErrorResponseHandler(new ObjectMapper());

    @Test
    void 미인증_요청에_공통_401_응답을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(
                new MockHttpServletRequest(),
                response,
                new InsufficientAuthenticationException("인증 필요")
        );

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains(
                "\"isSuccess\":false",
                "\"code\":\"COMMON_401_UNAUTHORIZED\"",
                "\"result\":null"
        );
    }

    @Test
    void 접근_거부에_공통_403_응답을_반환한다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
                new MockHttpServletRequest(),
                response,
                new AccessDeniedException("접근 거부")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains(
                "\"isSuccess\":false",
                "\"code\":\"COMMON_403_FORBIDDEN\"",
                "\"result\":null"
        );
    }
}
