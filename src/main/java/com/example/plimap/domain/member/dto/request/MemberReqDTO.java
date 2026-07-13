package com.example.plimap.domain.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

public class MemberReqDTO {

    @Getter
    public static class Onboarding {
        @NotBlank
        @Size(min = 2, max = 7)
        @Pattern(regexp = "^[가-힣A-Za-z0-9]+$")
        @Schema(description = "닉네임 (2~7자, 한글/영문/숫자)", example = "플리맵")
        private String nickname;

        @Schema(description = "프로필 이미지 객체 키", example = "profile/1/abc123.jpg")
        private String profileImageObjectKey;
    }
}
