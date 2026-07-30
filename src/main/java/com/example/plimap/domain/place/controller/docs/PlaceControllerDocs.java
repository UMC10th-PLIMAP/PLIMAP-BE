package com.example.plimap.domain.place.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
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
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Place", description = "장소 API")
public interface PlaceControllerDocs {

    @Operation(
            summary = "장소 검색",
            description = "Kakao Local REST API로 주소를 먼저 검색하고, 주소 결과가 없으면 "
                    + "키워드와 현재 위치를 기준으로 장소를 검색합니다. "
                    + "검색 결과는 저장하지 않습니다. "
                    + "(Figma 기준 화면: MP-02-01, MP-02-01-b, PN-02-01, PN-02-01-b)"
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
                    description = "Kakao 주소 또는 장소 검색 연동 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "504",
                    description = "Kakao 주소 또는 장소 검색 응답 지연")
    })
    ResponseEntity<ApiResponse<PlaceResponse.SearchResult>> searchPlaces(
            @Parameter(description = "검색어", required = true, example = "한강")
            String keyword,
            @Parameter(description = "사용자 현재 위도", required = true, example = "37.5283")
            @DecimalMin(value = "-90", message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            Double latitude,
            @Parameter(description = "사용자 현재 경도", required = true, example = "126.9326")
            @DecimalMin(value = "-180", message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            Double longitude
    );

    @Operation(
            summary = "검색 장소 선택",
            description = "Kakao 장소 검색 결과를 활성 PLACE_SEARCH Place와 매핑하거나 "
                    + "새 Place로 생성하고 주소·거리·PIN·북마크 정보를 반환합니다. "
                    + "선택 성공 시 인증 사용자의 최근 검색 장소 이력을 저장합니다. "
                    + "(Figma 기준 화면: MP-02-02-a, MP-02-02-b, MP-02-02-c, "
                    + "MP-02-02-d, PN-02-03-a)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "검색 장소 선택 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "필수 장소 정보 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패")
    })
    ResponseEntity<ApiResponse<PlaceResponse.Selection>> selectSearchPlace(
            @AuthenticationPrincipal AuthMember currentMember,
            PlaceRequest.Selection request
    );

    @Operation(
            summary = "최근 검색 장소 목록 조회",
            description = "장소 선택 성공 시 저장된 인증 사용자의 최근 검색 장소를 "
                    + "최신 선택순으로 최대 5개 조회합니다. "
                    + "Soft Delete된 Place의 이력은 제외하며 과거 이력으로 채우지 않습니다. "
                    + "(Figma 기준 화면: MP-02-01, PN-02-01)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "최근 검색 장소 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "현재 위치 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패")
    })
    ResponseEntity<ApiResponse<PlaceResponse.SearchHistoryResult>> getSearchHistories(
            @AuthenticationPrincipal AuthMember currentMember,
            @Parameter(description = "사용자 현재 위도", required = true, example = "37.5283")
            @NotNull(message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            @DecimalMin(value = "-90", message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "90", message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            Double latitude,
            @Parameter(description = "사용자 현재 경도", required = true, example = "126.9326")
            @NotNull(message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            @DecimalMin(value = "-180", message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            @DecimalMax(value = "180", message = PlaceRequest.INVALID_LOCATION_MESSAGE)
            Double longitude
    );

    @Operation(
            summary = "최근 검색 장소 삭제",
            description = "인증 사용자가 소유한 최근 검색 장소 이력을 삭제합니다. "
                    + "이력 삭제는 Place에 영향을 주지 않으며 과거 이력을 다시 채우지 않습니다. "
                    + "(Figma 기준 화면: MP-02-01, PN-02-01)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "최근 검색 장소 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "이력이 없거나 다른 사용자가 소유한 이력")
    })
    ResponseEntity<ApiResponse<Void>> deleteSearchHistory(
            @AuthenticationPrincipal AuthMember currentMember,
            @Parameter(description = "최근 검색 이력 ID", required = true, example = "10")
            Long historyId
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
