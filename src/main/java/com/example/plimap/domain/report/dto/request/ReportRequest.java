package com.example.plimap.domain.report.dto.request;

import com.example.plimap.domain.report.enums.ReportCategory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public final class ReportRequest {

    private static final String INVALID_DETAIL_MESSAGE = "신고 상세 내용이 카테고리 조건에 맞지 않습니다.";

    private ReportRequest() {
    }

    @Schema(name = "ReportCreateRequest")
    public record Create(
            @NotNull(message = "신고 카테고리를 입력해주세요.")
            @Schema(description = "신고 카테고리", example = "OTHER")
            ReportCategory category,

            @Schema(description = "기타 신고 상세 내용", example = "신고 사유")
            String detail
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