package com.example.plimap.global.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorApiResponse", description = "API 실패 응답")
public record ErrorApiResponse(
        @Schema(description = "요청 성공 여부", example = "false")
        Boolean isSuccess,

        @Schema(description = "에러 코드", example = "COMMON_400_VALIDATION_FAILED")
        String code,

        @Schema(description = "에러 메시지", example = "요청 값이 올바르지 않습니다.")
        String message,

        @Schema(description = "실패 응답에서는 항상 null", nullable = true, example = "null")
        Object result
) {
}
