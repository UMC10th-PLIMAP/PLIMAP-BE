package com.example.plimap.domain.track.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Track", description = "음악 API")
public interface PlaceTrackControllerDocs {

    @Operation(
            summary = "장소별 곡 목록 조회",
            description = "장소 정보와 해당 장소의 활성 PIN에 등록된 곡 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "장소별 곡 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "조회 조건 검증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples
                                            .PLACE_TRACK_VALIDATION_FAILED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "장소 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.PLACE_NOT_FOUND
                            )
                    ))
    })
    ResponseEntity<ApiResponse<PlaceTrackResponse.PlaceTrackListResult>> getPlaceTracks(
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
            @Parameter(
                    description = "사용자 현재 위도(-90 이상 90 이하)",
                    required = true,
                    example = "37.5665")
            @DecimalMin(value = "-90", message = "위도는 -90 이상이어야 합니다.")
            @DecimalMax(value = "90", message = "위도는 90 이하여야 합니다.")
            double latitude,
            @Parameter(
                    description = "사용자 현재 경도(-180 이상 180 이하)",
                    required = true,
                    example = "126.9780")
            @DecimalMin(value = "-180", message = "경도는 -180 이상이어야 합니다.")
            @DecimalMax(value = "180", message = "경도는 180 이하여야 합니다.")
            double longitude
    );
}
