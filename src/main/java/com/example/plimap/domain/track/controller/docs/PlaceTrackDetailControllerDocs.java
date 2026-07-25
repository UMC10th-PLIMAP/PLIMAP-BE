package com.example.plimap.domain.track.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.track.dto.response.PlaceTrackResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Track", description = "음악 API")
public interface PlaceTrackDetailControllerDocs {

    @Operation(
            summary = "장소 노래 상세 조회",
            description = "장소 노래의 Track 정보와 현재 사용자의 좋아요 여부를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "장소 노래 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "장소 노래 ID 타입 오류",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackDetail"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples
                                            .PLACE_TRACK_DETAIL_TYPE_MISMATCH
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackDetail"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "장소 노래 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackDetail"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples
                                            .PLACE_TRACK_NOT_FOUND
                            )
                    ))
    })
    ResponseEntity<ApiResponse<PlaceTrackResponse.PlaceTrackDetail>>
            getPlaceTrackDetail(
                    @AuthenticationPrincipal AuthMember currentMember,
                    @Parameter(description = "장소 노래 ID", required = true)
                    @Positive(message = "장소 노래 ID는 양수여야 합니다.")
                    Long placeTrackId
            );
}
