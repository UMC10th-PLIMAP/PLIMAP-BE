package com.example.plimap.domain.place.controller.docs;

import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Place", description = "장소 API")
public interface PlaceControllerDocs {

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
            PlaceRequest.MapSelection request
    );
}
