package com.example.plimap.domain.place.dto.response;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public final class PlaceResponse {

    private PlaceResponse() {
    }

    public record SearchResult(
            @Schema(description = "장소 검색 결과 목록")
            List<SearchItem> items
    ) {

        public SearchResult {
            items = List.copyOf(items);
        }
    }

    public record SearchItem(
            @Schema(description = "장소 검색 provider", example = "KAKAO")
            String provider,
            @Schema(description = "provider가 제공하는 장소 ID", example = "26338954")
            String providerPlaceId,
            @Schema(description = "장소명", example = "한강")
            String placeName,
            @Schema(description = "장소 카테고리", example = "여행 > 관광,명소 > 공원")
            String category,
            @Schema(description = "지번 주소", example = "서울특별시 영등포구 여의도동")
            String address,
            @Schema(description = "도로명 주소", example = "서울특별시 영등포구 여의동로")
            String roadAddress,
            @Schema(description = "장소 위도", example = "37.5283")
            Double latitude,
            @Schema(description = "장소 경도", example = "126.9326")
            Double longitude,
            @Schema(description = "사용자 현재 위치 기준 거리(m)", example = "470")
            Integer distanceMeters,
            @Schema(description = "활성 PIN 존재 여부", example = "true")
            boolean hasPin,
            @Schema(description = "최초 PIN 작성자 닉네임", example = "홍길동", nullable = true)
            String firstPinCreatorNickname
    ) {
    }

    public record Selection(
            @Schema(description = "장소 ID", example = "1")
            Long placeId,
            @Schema(description = "장소명", example = "한강")
            String placeName,
            @Schema(description = "축약하지 않은 전체 지번 주소", example = "서울특별시 영등포구 여의도동")
            String address,
            @Schema(
                    description = "축약하지 않은 전체 도로명 주소. 없는 경우 null이며 address를 사용",
                    example = "서울특별시 영등포구 여의동로",
                    nullable = true
            )
            String roadAddress,
            @Schema(description = "장소 생성 출처", example = "PLACE_SEARCH")
            PlaceSource source,
            @Schema(description = "사용자 현재 위치 기준 거리(m)", example = "470")
            Integer distanceMeters,
            @Schema(description = "사용자가 500m 이내에 있는지 여부", example = "true")
            boolean withinAccessRange,
            @Schema(description = "활성 PIN 존재 여부", example = "true")
            boolean hasPin,
            @Schema(
                    description = "최초 활성 PIN 작성자 닉네임",
                    example = "홍길동",
                    nullable = true
            )
            String firstPinCreatorNickname,
            @Schema(description = "활성 PIN 개수", example = "3")
            Long pinCount,
            @Schema(description = "인증 사용자의 장소 북마크 여부", example = "false")
            boolean bookmarkedByMe
    ) {
    }

    public record MapSelection(
            @Schema(description = "장소 ID", example = "12")
            Long placeId,
            @Schema(description = "장소명", example = "물빛무대 앞 광장")
            String placeName,
            @Schema(description = "장소 생성 출처", example = "MAP_SELECTION")
            PlaceSource source,
            @Schema(description = "장소 위도", example = "37.5283")
            Double latitude,
            @Schema(description = "장소 경도", example = "126.9326")
            Double longitude
    ) {

        public static MapSelection from(Place place) {
            return new MapSelection(
                    place.getId(),
                    place.getName(),
                    place.getSource(),
                    place.getLocation().getY(),
                    place.getLocation().getX()
            );
        }
    }
}
