package com.example.plimap.global.external.kakao;

import com.example.plimap.global.external.kakao.dto.KakaoAddressResponse;
import com.example.plimap.global.external.kakao.dto.KakaoRegionCodeResponse;

public interface KakaoCoordinateClient {

    KakaoRegionCodeResponse getRegionCodes(double latitude, double longitude);

    KakaoAddressResponse getAddress(double latitude, double longitude);
}
