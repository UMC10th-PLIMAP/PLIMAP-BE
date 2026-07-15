package com.example.plimap.global.security;

import com.example.plimap.global.apiPayload.ApiResponse;
import com.example.plimap.global.apiPayload.code.GeneralErrorCode;
import com.example.plimap.global.logging.HttpErrorLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authenticationException) throws IOException {
        HttpErrorLogger.info(request, GeneralErrorCode.UNAUTHORIZED, authenticationException);
        writeErrorResponse(response, GeneralErrorCode.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        HttpErrorLogger.warn(request, GeneralErrorCode.FORBIDDEN, accessDeniedException);
        writeErrorResponse(response, GeneralErrorCode.FORBIDDEN);
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    GeneralErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(errorCode));
    }
}
