package com.example.plimap.domain.home.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.home.dto.response.HomeResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Home", description = "홈 화면 API")
public interface HomeControllerDocs {

    @Operation(
            summary = "홈 컨텍스트 조회",
            description = "인증 사용자의 닉네임과 현재 좌표의 Kakao 행정동(H) 정보를 조회합니다. "
                    + "행정동 결과가 없으면 지역 필드를 null로 반환합니다. 위치 권한 거부는 "
                    + "프론트에서 처리하며 API 오류가 아닙니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "HOME_200_CONTEXT_FETCHED - 홈 컨텍스트 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "COMMON_400_VALIDATION_FAILED - 현재 위치 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "COMMON_401_UNAUTHORIZED - 인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "PLACE_EXTERNAL_API_ERROR - Kakao 행정구역 연동 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "504",
                    description = "PLACE_EXTERNAL_API_TIMEOUT - Kakao 행정구역 응답 지연")
    })
    ResponseEntity<ApiResponse<HomeResponse.Context>> getHomeContext(
            @AuthenticationPrincipal
            @Parameter(hidden = true)
            AuthMember currentMember,
            @Parameter(description = "사용자 현재 위도", required = true, example = "37.5000")
            @NotNull(message = "위치 정보가 올바르지 않습니다.")
            @DecimalMin(value = "-90", message = "위치 정보가 올바르지 않습니다.")
            @DecimalMax(value = "90", message = "위치 정보가 올바르지 않습니다.")
            Double latitude,
            @Parameter(description = "사용자 현재 경도", required = true, example = "127.0300")
            @NotNull(message = "위치 정보가 올바르지 않습니다.")
            @DecimalMin(value = "-180", message = "위치 정보가 올바르지 않습니다.")
            @DecimalMax(value = "180", message = "위치 정보가 올바르지 않습니다.")
            Double longitude
    );
}
