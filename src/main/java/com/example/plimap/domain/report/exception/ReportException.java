package com.example.plimap.domain.report.exception;

import com.example.plimap.global.apiPayload.exception.BusinessException;

public class ReportException extends BusinessException {

    public ReportException(ReportErrorCode errorCode) {
        super(errorCode);
    }

    public ReportException(ReportErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
