package com.example.plimap.domain.admin.exception;

import com.example.plimap.global.apiPayload.exception.BusinessException;

public class AdminException extends BusinessException {

    public AdminException(AdminErrorCode errorCode) {
        super(errorCode);
    }
}
