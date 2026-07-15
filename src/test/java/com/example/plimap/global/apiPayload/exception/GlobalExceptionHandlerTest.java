package com.example.plimap.global.apiPayload.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void BindException은_첫_번째_필드_오류_메시지로_400을_반환한다() throws Exception {
        mockMvc.perform(get("/exception-test/bind"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("첫 번째 필드 오류입니다."))
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    void BindException의_필드_오류_메시지가_없으면_기본_메시지를_반환한다() throws Exception {
        mockMvc.perform(get("/exception-test/bind-without-message"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("요청 값이 올바르지 않습니다."));
    }

    @Test
    void BindException은_클래스_오류보다_필드_오류_메시지를_우선한다() throws Exception {
        mockMvc.perform(get("/exception-test/bind-with-global-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("필드 오류입니다."));
    }

    @Test
    void 필수_요청_헤더가_누락되면_헤더_이름을_포함한_400을_반환한다() throws Exception {
        mockMvc.perform(get("/exception-test/required-header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400_MISSING_HEADER"))
                .andExpect(jsonPath("$.message")
                        .value("필수 요청 헤더 'X-Device-Id'가 누락되었습니다."))
                .andExpect(jsonPath("$.result").isEmpty());
    }

    @Test
    void 그_외_ServletRequestBindingException은_기존_400을_반환한다() throws Exception {
        mockMvc.perform(get("/exception-test/servlet-request-binding"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_BAD_REQUEST"));
    }

    @RestController
    static class TestController {

        @GetMapping("/exception-test/bind")
        void bind() throws BindException {
            BindException exception = new BindException(new Object(), "request");
            exception.addError(new FieldError(
                    "request",
                    "firstField",
                    "첫 번째 필드 오류입니다."
            ));
            exception.addError(new FieldError(
                    "request",
                    "secondField",
                    "두 번째 필드 오류입니다."
            ));
            throw exception;
        }

        @GetMapping("/exception-test/bind-without-message")
        void bindWithoutMessage() throws BindException {
            BindException exception = new BindException(new Object(), "request");
            exception.addError(new FieldError(
                    "request",
                    "field",
                    null,
                    false,
                    null,
                    null,
                    null
            ));
            throw exception;
        }

        @GetMapping("/exception-test/bind-with-global-error")
        void bindWithGlobalError() throws BindException {
            BindException exception = new BindException(new Object(), "request");
            exception.addError(new ObjectError("request", "클래스 오류입니다."));
            exception.addError(new FieldError("request", "field", "필드 오류입니다."));
            throw exception;
        }

        @GetMapping("/exception-test/required-header")
        void requiredHeader(@RequestHeader("X-Device-Id") String deviceId) {
        }

        @GetMapping("/exception-test/servlet-request-binding")
        void servletRequestBinding() throws ServletRequestBindingException {
            throw new ServletRequestBindingException("요청 바인딩 오류");
        }
    }
}
