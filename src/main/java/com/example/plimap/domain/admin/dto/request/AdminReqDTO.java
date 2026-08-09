package com.example.plimap.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public final class AdminReqDTO {

    private AdminReqDTO() {
    }

    @Schema(name = "AdminPenaltyDecisionRequest")
    public record PenaltyDecision(
            @NotNull(message = "벌점 부여 여부를 입력해주세요.")
            @Schema(description = "벌점 부여 여부", example = "true")
            Boolean grantPenalty
    ) {
    }
}
