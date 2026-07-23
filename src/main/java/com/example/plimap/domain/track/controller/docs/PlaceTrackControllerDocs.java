package com.example.plimap.domain.track.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Track", description = "음악 API")
public interface PlaceTrackControllerDocs {

    @Operation(
            summary = "장소별 곡 목록 조회",
            description = "장소 정보와 해당 장소의 공개 PIN에 등록된 곡 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "장소별 곡 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "조회 조건 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "장소 없음")
    })
    ResponseEntity<ApiResponse<PlaceTrackResponse.ListResult>> getPlaceTracks(
            @AuthenticationPrincipal AuthMember currentMember,
            @Parameter(description = "장소 ID", required = true)
            @Positive(message = "장소 ID는 양수여야 합니다.")
            Long placeId,
            @Parameter(description = "정렬 기준(POPULAR, LATEST)")
            PlaceTrackSort sort,
            @Parameter(description = "페이지 번호(0 이상)")
            @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
            int page,
            @Parameter(description = "페이지 크기(1~200)")
            @Min(value = 1, message = "페이지 크기는 1개 이상이어야 합니다.")
            @Max(value = 200, message = "페이지 크기는 200개 이하여야 합니다.")
            int size,
            @Parameter(description = "사용자 현재 위도", required = true)
            double latitude,
            @Parameter(description = "사용자 현재 경도", required = true)
            double longitude
    );
}
