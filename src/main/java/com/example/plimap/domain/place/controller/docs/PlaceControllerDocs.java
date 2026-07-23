package com.example.plimap.domain.place.controller.docs;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.http.ResponseEntity;

@Tag(name = "Place", description = "장소 API")
public interface PlaceControllerDocs {

    @Operation(
            summary = "장소 검색",
            description = "Kakao Local REST API를 통해 키워드와 현재 위치를 기준으로 장소를 검색합니다. "
                    + "검색 결과는 저장하지 않습니다. (화면: MP-02-01-a, MP-02-01-b)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "장소 검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "검색어 또는 현재 위치 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "502",
                    description = "Kakao 장소 검색 연동 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "504",
                    description = "Kakao 장소 검색 응답 지연")
    })
    ResponseEntity<ApiResponse<PlaceResponse.SearchResult>> searchPlaces(
            @Parameter(description = "검색어", required = true, example = "한강")
            String keyword,
            @Parameter(description = "사용자 현재 위도", required = true, example = "37.5283")
            @DecimalMin(value = "-90", message = "위치 정보가 올바르지 않습니다.")
            @DecimalMax(value = "90", message = "위치 정보가 올바르지 않습니다.")
            Double latitude,
            @Parameter(description = "사용자 현재 경도", required = true, example = "126.9326")
            @DecimalMin(value = "-180", message = "위치 정보가 올바르지 않습니다.")
            @DecimalMax(value = "180", message = "위치 정보가 올바르지 않습니다.")
            Double longitude
    );

    @Operation(
            summary = "지도 선택 장소 확정",
            description = "지도에서 선택한 위치를 기존 MAP_SELECTION Place와 매핑하거나 새 Place로 생성합니다. "
                    + "(Figma 기준 화면: PN-02-03)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "지도 선택 장소 확정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "좌표 또는 주소 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패")
    })
    ResponseEntity<ApiResponse<PlaceResponse.MapSelection>> confirmMapSelection(
            @Valid PlaceRequest.MapSelection request
    );
}
