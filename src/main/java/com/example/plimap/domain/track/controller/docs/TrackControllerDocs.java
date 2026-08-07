package com.example.plimap.domain.track.controller.docs;

import com.example.plimap.domain.track.dto.request.TrackRequest;
import com.example.plimap.domain.track.dto.response.TrackResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "Track", description = "음악 API")
public interface TrackControllerDocs {

    @Operation(
            summary = "음악 검색",
            description = "iTunes Search API를 통해 곡명 또는 아티스트명으로 음악을 검색합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "음악 검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "검색어 또는 검색 결과 개수 검증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponseTrackSearchResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples
                                            .SEARCH_VALIDATION_FAILED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponseTrackSearchResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "iTunes 외부 API 호출 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponseTrackSearchResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples
                                            .TRACK_EXTERNAL_API_ERROR
                            )
                    ))
    })
    ResponseEntity<ApiResponse<TrackResponse.TrackSearchResult>> searchTracks(
            @Parameter(description = "곡명 또는 아티스트명", required = true)
            @NotBlank(message = "검색어를 입력해주세요.")
            String keyword,
            @Parameter(description = "검색 결과 개수(1~200), 기본값 20")
            @Min(value = 1, message = "검색 결과 개수는 1개 이상이어야 합니다.")
            @Max(value = 200, message = "검색 결과 개수는 200개 이하여야 합니다.")
            int limit
    );

    @Operation(
            summary = "구간 재생 준비",
            description = "iTunes 곡 메타데이터를 기반으로 YouTube 영상을 매칭합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "구간 재생 준비 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "iTunes 트랙 ID 검증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaybackPreparationResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples
                                            .PLAYBACK_VALIDATION_FAILED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaybackPreparationResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "메타데이터 만료 또는 YouTube 매칭 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaybackPreparationResult"),
                            examples = {
                                    @ExampleObject(
                                            name = "메타데이터 만료",
                                            value = TrackSwaggerErrorExamples
                                                .TRACK_METADATA_CACHE_NOT_FOUND
                                    ),
                                    @ExampleObject(
                                            name = "YouTube 매칭 실패",
                                            value = TrackSwaggerErrorExamples
                                                .YOUTUBE_MATCH_NOT_FOUND
                                    )
                            }
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "YouTube 외부 API 또는 캐시 처리 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaybackPreparationResult"),
                            examples = {
                                    @ExampleObject(
                                            name = "YouTube 외부 API 오류",
                                            value = TrackSwaggerErrorExamples
                                                .YOUTUBE_EXTERNAL_API_ERROR
                                    ),
                                    @ExampleObject(
                                            name = "캐시 처리 오류",
                                            value = TrackSwaggerErrorExamples
                                                .TRACK_CACHE_ERROR
                                    )
                            }
                    ))
    })
    ResponseEntity<ApiResponse<TrackResponse.PlaybackPreparationResult>> preparePlayback(
            @Valid TrackRequest.PlaybackPreparation request
    );

    @Operation(
            summary = "YouTube 재생 실패 보고",
            description = "IFrame Player 오류 코드를 분류하고 확정적인 재생 실패를 24시간 캐시합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "YouTube 재생 실패 보고 성공",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiResponseVoid"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples
                                            .PLAYBACK_FAILURE_REPORTED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "재생 실패 요청 검증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiResponseVoid"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples
                                            .PLAYBACK_FAILURE_VALIDATION_FAILED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiResponseVoid"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "캐시 처리 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ApiResponseVoid"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.TRACK_CACHE_ERROR
                            )
                    ))
    })
    ResponseEntity<ApiResponse<Void>> reportPlaybackFailure(
            @Valid TrackRequest.PlaybackFailure request
    );
}
