package com.example.plimap.global.logging;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class HttpErrorLogger {

    private static final String LOG_FORMAT =
            "status={} code={} method={} uri={} exception={}";

    private HttpErrorLogger() {
    }

    public static void info(HttpServletRequest request,
                            BaseErrorCode errorCode,
                            Throwable exception) {
        log.info(
                LOG_FORMAT,
                errorCode.getStatus().value(),
                errorCode.getCode(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
    }

    public static void warn(HttpServletRequest request,
                            BaseErrorCode errorCode,
                            Throwable exception) {
        log.warn(
                LOG_FORMAT,
                errorCode.getStatus().value(),
                errorCode.getCode(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName()
        );
    }

    public static void error(HttpServletRequest request,
                             BaseErrorCode errorCode,
                             Throwable exception) {
        log.error(
                LOG_FORMAT,
                errorCode.getStatus().value(),
                errorCode.getCode(),
                request.getMethod(),
                request.getRequestURI(),
                exception.getClass().getSimpleName(),
                exception
        );
    }
}
