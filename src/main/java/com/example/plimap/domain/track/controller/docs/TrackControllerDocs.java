package com.example.plimap.domain.track.controller.docs;

import com.example.plimap.domain.track.dto.response.TrackResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
                    description = "검색어 또는 검색 결과 개수 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "iTunes 외부 API 호출 실패")
    })
    ResponseEntity<ApiResponse<TrackResponse.SearchResult>> searchTracks(
            @Parameter(description = "곡명 또는 아티스트명", required = true)
            @NotBlank(message = "검색어를 입력해주세요.")
            String keyword,
            @Parameter(description = "검색 결과 개수(1~200), 기본값 20")
            @Min(value = 1, message = "검색 결과 개수는 1개 이상이어야 합니다.")
            @Max(value = 200, message = "검색 결과 개수는 200개 이하여야 합니다.")
            int limit
    );
}
