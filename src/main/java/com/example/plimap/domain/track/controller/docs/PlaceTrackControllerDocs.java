package com.example.plimap.domain.track.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.track.dto.request.PlaceTrackRequest;
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
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Track", description = "음악 API")
public interface PlaceTrackControllerDocs {

    @Operation(
            summary = "좋아요한 장소별 곡 목록 조회",
            description = "현재 사용자가 좋아요한 활성 장소별 곡 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요한 장소별 곡 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "페이지 조건 검증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponseLikedPlaceTrackListResult"),
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
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponseLikedPlaceTrackListResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.UNAUTHORIZED
                            )
                    ))
    })
    ResponseEntity<ApiResponse<PlaceTrackResponse.LikedPlaceTrackListResult>>
            getLikedPlaceTracks(
                    @AuthenticationPrincipal AuthMember currentMember,
                    @Parameter(description = "페이지 번호(0 이상)")
                    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
                    int page,
                    @Parameter(description = "페이지 크기(1~200)")
                    @Min(value = 1, message = "페이지 크기는 1개 이상이어야 합니다.")
                    @Max(value = 200, message = "페이지 크기는 200개 이하여야 합니다.")
                    int size
            );

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
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackListResult"),
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
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackListResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "장소 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackListResult"),
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
                    description = "장소 노래 ID 타입 또는 양수 검증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackDetail"),
                            examples = {
                                    @ExampleObject(
                                            name = "타입 불일치",
                                            value = TrackSwaggerErrorExamples
                                                .PLACE_TRACK_DETAIL_TYPE_MISMATCH
                                    ),
                                    @ExampleObject(
                                            name = "양수 검증 실패",
                                            value = TrackSwaggerErrorExamples
                                                .PLACE_TRACK_DETAIL_VALIDATION_FAILED
                                    )
                            }
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
                    responseCode = "403",
                    description = "장소별 곡 상세 접근 권한 없음"),
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
                    Long placeTrackId,
                    @ModelAttribute @Valid PlaceTrackRequest.UserLocation request,
                    @RequestHeader(value = "Place-Access-Token", required = false)
                    String token
            );

    @Operation(
            summary = "장소별 곡 좋아요 등록",
            description = "현재 사용자가 장소별 곡에 좋아요를 등록합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "장소별 곡 좋아요 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "장소별 곡 ID 검증 실패 또는 중복 좋아요",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackLikeResult"),
                            examples = {
                                    @ExampleObject(
                                            name = "타입 불일치",
                                            value = TrackSwaggerErrorExamples
                                                .PLACE_TRACK_DETAIL_TYPE_MISMATCH
                                    ),
                                    @ExampleObject(
                                            name = "양수 검증 실패",
                                            value = TrackSwaggerErrorExamples
                                                .PLACE_TRACK_DETAIL_VALIDATION_FAILED
                                    ),
                                    @ExampleObject(
                                            name = "중복 좋아요",
                                            value = TrackSwaggerErrorExamples
                                                .PLACE_TRACK_ALREADY_LIKED
                                    )
                            }
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackLikeResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "장소별 곡 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackLikeResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples
                                        .PLACE_TRACK_NOT_FOUND
                            )
                    ))
    })
    ResponseEntity<ApiResponse<PlaceTrackResponse.PlaceTrackLikeResult>>
            createPlaceTrackLike(
                    @AuthenticationPrincipal AuthMember currentMember,
                    @Parameter(description = "장소별 곡 ID", required = true)
                    @Positive(message = "장소별 곡 ID는 양수여야 합니다.")
                    Long placeTrackId
            );

    @Operation(
            summary = "장소별 곡 좋아요 삭제",
            description = "현재 사용자가 등록한 장소별 곡 좋아요를 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "장소별 곡 좋아요 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "장소별 곡 ID 검증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackLikeResult"),
                            examples = {
                                    @ExampleObject(
                                            name = "타입 불일치",
                                            value = TrackSwaggerErrorExamples
                                                .PLACE_TRACK_DETAIL_TYPE_MISMATCH
                                    ),
                                    @ExampleObject(
                                            name = "양수 검증 실패",
                                            value = TrackSwaggerErrorExamples
                                                .PLACE_TRACK_DETAIL_VALIDATION_FAILED
                                    )
                            }
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackLikeResult"),
                            examples = @ExampleObject(
                                    value = TrackSwaggerErrorExamples.UNAUTHORIZED
                            )
                    )),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "장소별 곡 또는 좋아요 없음",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    ref = "#/components/schemas/ApiResponsePlaceTrackLikeResult"),
                            examples = {
                                    @ExampleObject(
                                            name = "장소별 곡 없음",
                                            value = TrackSwaggerErrorExamples
                                                .PLACE_TRACK_NOT_FOUND
                                    ),
                                    @ExampleObject(
                                            name = "좋아요 없음",
                                            value = TrackSwaggerErrorExamples
                                                .PLACE_TRACK_LIKE_NOT_FOUND
                                    )
                            }
                    ))
    })
    ResponseEntity<ApiResponse<PlaceTrackResponse.PlaceTrackLikeResult>>
            deletePlaceTrackLike(
                    @AuthenticationPrincipal AuthMember currentMember,
                    @Parameter(description = "장소별 곡 ID", required = true)
                    @Positive(message = "장소별 곡 ID는 양수여야 합니다.")
                    Long placeTrackId
            );
}
