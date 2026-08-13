package com.example.plimap.domain.home.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.home.dto.response.HomeResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import com.example.plimap.global.swagger.CommonSwaggerErrorExamples;
import com.example.plimap.global.swagger.ErrorApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
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
                    description = "HOME_CONTEXT_FETCHED_SUCCESS - 홈 컨텍스트 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "현재 위치 좌표 검증에 실패한 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "COMMON_400_VALIDATION_FAILED",
                                    summary = "현재 위치 검증 실패",
                                    value = HomeSwaggerErrorExamples.LOCATION_VALIDATION_FAILED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 정보가 없거나 유효하지 않은 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "COMMON_401_UNAUTHORIZED",
                                    summary = "인증 필요",
                                    value = CommonSwaggerErrorExamples.UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "Kakao 행정구역 연동에 실패한 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "PLACE_EXTERNAL_API_ERROR",
                                    summary = "외부 장소 API 연동 실패",
                                    value = HomeSwaggerErrorExamples.PLACE_EXTERNAL_API_ERROR
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "504",
                    description = "Kakao 행정구역 응답이 지연된 경우",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorApiResponse.class),
                            examples = @ExampleObject(
                                    name = "PLACE_EXTERNAL_API_TIMEOUT",
                                    summary = "외부 장소 API 응답 지연",
                                    value = HomeSwaggerErrorExamples.PLACE_EXTERNAL_API_TIMEOUT
                            )
                    ))
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
