package com.example.plimap.global.apiPayload;

import com.example.plimap.global.apiPayload.code.BaseErrorCode;
import com.example.plimap.global.apiPayload.code.BaseSuccessCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.util.Objects;

@Getter
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public final class ApiResponse<T> {

    private final Boolean isSuccess;

    private final String code;

    private final String message;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    private final T result;

    private ApiResponse(Boolean isSuccess, String code, String message, T result) {
        this.isSuccess = isSuccess;
        this.code = code;
        this.message = message;
        this.result = result;
    }

    public static <T> ApiResponse<T> success(BaseSuccessCode successCode, T result) {
        Objects.requireNonNull(successCode, "successCode must not be null");

        return new ApiResponse<>(
                true,
                successCode.getCode(),
                successCode.getMessage(),
                result
        );
    }

    public static ApiResponse<Void> failure(BaseErrorCode errorCode) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");

        return failure(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> failure(BaseErrorCode errorCode, String message) {
        Objects.requireNonNull(errorCode, "errorCode must not be null");
        Objects.requireNonNull(message, "message must not be null");

        return new ApiResponse<>(false, errorCode.getCode(), message, null);
    }
}
