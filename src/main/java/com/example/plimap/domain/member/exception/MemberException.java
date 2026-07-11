package com.example.plimap.domain.member.exception;

import com.example.plimap.global.apiPayload.exception.BusinessException;

public class MemberException extends BusinessException {

    public MemberException(MemberErrorCode errorCode) {
        super(errorCode);
    }
}
