package com.example.plimap.global.external.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KakaoRegionCodeResponse(
        List<Document> documents
) {

    public KakaoRegionCodeResponse {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    public record Document(
            @JsonProperty("region_type")
            String regionType,
            String code,
            @JsonProperty("region_1depth_name")
            String region1DepthName,
            @JsonProperty("region_2depth_name")
            String region2DepthName,
            @JsonProperty("region_3depth_name")
            String region3DepthName
    ) {
    }
}
