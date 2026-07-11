package com.example.plimap.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

public class AuthReqDTO {

    @Getter
    public static class TempToken {
        @NotNull
        @Schema(description = "토큰을 발급할 멤버 ID", example = "1")
        private Long memberId;
    }
}
