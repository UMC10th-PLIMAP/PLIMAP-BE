package com.example.plimap.global.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KakaoAddressSearchResponse(
        List<Document> documents
) {

    public KakaoAddressSearchResponse {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    public record Document(
            @JsonProperty("address_name")
            String addressName,
            String x,
            String y,
            Address address,
            @JsonProperty("road_address")
            RoadAddress roadAddress
    ) {
    }

    public record Address(
            @JsonProperty("address_name")
            String addressName
    ) {
    }

    public record RoadAddress(
            @JsonProperty("address_name")
            String addressName
    ) {
    }
}
