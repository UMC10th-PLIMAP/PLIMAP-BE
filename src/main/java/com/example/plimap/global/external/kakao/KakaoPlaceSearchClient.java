package com.example.plimap.global.external.kakao;

import com.example.plimap.global.external.kakao.dto.KakaoPlaceSearchResponse;

public interface KakaoPlaceSearchClient {

    KakaoPlaceSearchResponse search(String keyword, double latitude, double longitude);
}
