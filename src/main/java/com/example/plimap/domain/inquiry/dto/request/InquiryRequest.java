package com.example.plimap.domain.inquiry.dto.request;

import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class InquiryRequest {

    private InquiryRequest() {
    }

    @Schema(name = "InquiryCreateRequest")
    public record Create(
            @NotNull(message = "문의 카테고리를 입력해주세요.")
            @Schema(description = "문의 카테고리", example = "APP_BUG_OR_ERROR")
            InquiryCategory category,

            @NotBlank(message = "문의 제목을 입력해주세요.")
            @Size(max = 100, message = "문의 제목은 100자 이하로 입력해주세요.")
            @Schema(description = "문의 제목", example = "핀 재생이 안 돼요")
            String title,

            @NotBlank(message = "문의 내용을 입력해주세요.")
            @Schema(description = "문의 내용", example = "특정 곡의 PIN이 재생되지 않습니다.")
            String content,

            @NotBlank(message = "답변받을 이메일을 입력해주세요.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            @Size(max = 320, message = "답변받을 이메일은 320자 이하로 입력해주세요.")
            @Schema(description = "답변받을 이메일 (로그인 사용자의 가입 이메일 자동 입력은 프론트엔드가 담당하며, 수정 가능)", example = "user@example.com")
            String contactEmail
    ) {
    }
}
