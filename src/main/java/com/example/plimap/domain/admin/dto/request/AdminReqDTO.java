package com.example.plimap.domain.admin.dto.request;

import com.example.plimap.domain.member.enums.SuspensionPeriod;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public final class AdminReqDTO {

    private static final String INVALID_DETAIL_MESSAGE = "제재 사유 상세 내용이 카테고리 조건에 맞지 않습니다.";

    private AdminReqDTO() {
    }

    @Schema(name = "AdminPenaltyDecisionRequest")
    public record PenaltyDecision(
            @NotNull(message = "벌점 부여 여부를 입력해주세요.")
            @Schema(description = "벌점 부여 여부(반려 처리만 지원, false 고정)", example = "false")
            Boolean grantPenalty
    ) {
    }

    @Schema(name = "AdminPinSanctionRequest")
    public record PinSanctionDecision(
            @NotNull(message = "제재 사유로 삼을 신고를 선택해주세요.")
            @Schema(description = "제재 사유로 지목할 신고 ID(해당 PIN에 걸린 신고 중 하나)", example = "1")
            Long reportId,

            @NotNull(message = "제재 기간을 선택해주세요.")
            @Schema(description = "제재 기간", example = "THREE_DAYS")
            SuspensionPeriod period
    ) {
    }

    @Schema(name = "AdminMemberSanctionRequest")
    public record MemberSanctionDecision(
            @NotNull(message = "제재 사유 카테고리를 선택해주세요.")
            @Schema(description = "제재 사유 카테고리", example = "ABUSE_OR_HATE_SPEECH")
            ReportCategory category,

            @Schema(description = "제재 사유 상세(카테고리가 OTHER일 때만 입력)", example = "지속적인 혐오 발언")
            String detail,

            @NotNull(message = "제재 기간을 선택해주세요.")
            @Schema(description = "제재 기간", example = "PERMANENT")
            SuspensionPeriod period
    ) {

        @JsonIgnore
        @Schema(hidden = true)
        @AssertTrue(message = INVALID_DETAIL_MESSAGE)
        public boolean isDetailValid() {
            if (category == null) {
                return true;
            }
            if (category == ReportCategory.OTHER) {
                return detail != null && !detail.isBlank();
            }
            return detail == null;
        }
    }
}
