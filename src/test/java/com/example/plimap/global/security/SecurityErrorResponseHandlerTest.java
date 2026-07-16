package com.example.plimap.global.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
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

    @Test
    void 인증_실패는_공통_필드와_함께_INFO로_기록하고_민감정보는_제외한다(
            CapturedOutput output
    ) throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(
                request,
                response,
                new InsufficientAuthenticationException("token-secret")
        );

        assertThat(output.getAll())
                .contains(
                        "INFO",
                        "status=401 code=COMMON_401_UNAUTHORIZED "
                                + "method=POST uri=/api/v1/private "
                                + "exception=InsufficientAuthenticationException"
                )
                .doesNotContain(
                        "query-secret",
                        "authorization-secret",
                        "cookie-secret",
                        "token-secret"
                );
    }

    @Test
    void 접근_거부는_공통_필드와_함께_WARN으로_기록한다(CapturedOutput output)
            throws Exception {
        MockHttpServletRequest request = request("DELETE", "/api/v1/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("access-secret"));

        assertThat(output.getAll())
                .contains(
                        "WARN",
                        "status=403 code=COMMON_403_FORBIDDEN "
                                + "method=DELETE uri=/api/v1/private "
                                + "exception=AccessDeniedException"
                )
                .doesNotContain("access-secret");
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        request.setQueryString("access_token=query-secret");
        request.addHeader("Authorization", "Bearer authorization-secret");
        request.addHeader("Cookie", "refreshToken=cookie-secret");
        return request;
    }
}
