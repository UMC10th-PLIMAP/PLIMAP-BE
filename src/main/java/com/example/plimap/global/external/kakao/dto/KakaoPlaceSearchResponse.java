package com.example.plimap.global.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KakaoPlaceSearchResponse(
        List<Document> documents
) {

    public KakaoPlaceSearchResponse {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    public record Document(
            String id,
            @JsonProperty("place_name")
            String placeName,
            @JsonProperty("category_name")
            String categoryName,
            @JsonProperty("address_name")
            String addressName,
            @JsonProperty("road_address_name")
            String roadAddressName,
            String x,
            String y,
            String distance
    ) {
    }
}
