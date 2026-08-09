package com.example.plimap.global.external.kakao;

import com.example.plimap.global.external.kakao.dto.KakaoAddressSearchResponse;

public interface KakaoAddressSearchClient {

    KakaoAddressSearchResponse search(String query);
}
