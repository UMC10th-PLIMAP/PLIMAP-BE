package com.example.plimap.domain.home.dto.response;

import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import io.swagger.v3.oas.annotations.media.Schema;

public final class HomeResponse {

    private HomeResponse() {
    }

    @Schema(name = "HomeContextResponse")
    public record Context(
            @Schema(description = "로그인 사용자 닉네임", example = "델리만쥬", nullable = true)
            String nickname,
            @Schema(
                    description = "현재 위치 행정구역. 행정동 결과가 없어도 객체는 항상 존재합니다.",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            CurrentRegion currentRegion
    ) {
    }

    @Schema(name = "HomeCurrentRegionResponse")
    public record CurrentRegion(
            @Schema(description = "시/도", example = "서울특별시", nullable = true)
            String sido,
            @Schema(description = "시/군/구", example = "강남구", nullable = true)
            String sigungu,
            @Schema(description = "읍/면/동", example = "역삼1동", nullable = true)
            String eupMyeonDong,
            @Schema(
                    description = "시/도와 시/군/구를 공백으로 연결한 표시명",
                    example = "서울특별시 강남구",
                    nullable = true
            )
            String displayName
    ) {

        public static CurrentRegion from(PlaceAdministrativeRegion region) {
            String displayName = region.sido() == null || region.sigungu() == null
                    ? null
                    : region.sido() + " " + region.sigungu();
            return new CurrentRegion(
                    region.sido(),
                    region.sigungu(),
                    region.eupMyeonDong(),
                    displayName
            );
        }
    }
}
