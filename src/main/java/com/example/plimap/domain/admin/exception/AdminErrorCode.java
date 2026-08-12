package com.example.plimap.domain.admin.exception;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AdminErrorCode implements BaseErrorCode {

    PENALTY_GRANT_NOT_SUPPORTED(
            HttpStatus.BAD_REQUEST,
            "ADMIN_400_PENALTY_GRANT_NOT_SUPPORTED",
            "벌점 부여는 이 API에서 더 이상 지원하지 않습니다. 최종 제재 API를 이용해 주세요."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}
