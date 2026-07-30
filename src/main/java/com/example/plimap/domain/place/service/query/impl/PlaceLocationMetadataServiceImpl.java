package com.example.plimap.domain.place.service.query.impl;

import com.example.plimap.domain.place.dto.PlaceAddressDecision;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import com.example.plimap.global.external.kakao.KakaoClientException;
import com.example.plimap.global.external.kakao.KakaoClientTimeoutException;
import com.example.plimap.global.external.kakao.KakaoCoordinateClient;
import com.example.plimap.global.external.kakao.dto.KakaoAddressResponse;
import com.example.plimap.global.external.kakao.dto.KakaoRegionCodeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceLocationMetadataServiceImpl implements PlaceLocationMetadataService {

    private static final String ADMINISTRATIVE_REGION_TYPE = "H";
    private static final PlaceAdministrativeRegion EMPTY_REGION =
            new PlaceAdministrativeRegion(null, null, null, null);
    private static final PlaceAddressDecision EMPTY_ADDRESS_DECISION =
            new PlaceAddressDecision(null, null, null);

    private final KakaoCoordinateClient kakaoCoordinateClient;

    @Override
    public PlaceAdministrativeRegion getAdministrativeRegion(
            double latitude,
            double longitude
    ) {
        try {
            return kakaoCoordinateClient.getRegionCodes(latitude, longitude)
                    .documents()
                    .stream()
                    .filter(document ->
                            ADMINISTRATIVE_REGION_TYPE.equals(document.regionType()))
                    .findFirst()
                    .map(this::toAdministrativeRegion)
                    .orElse(EMPTY_REGION);
        } catch (KakaoClientTimeoutException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT, exception);
        } catch (KakaoClientException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR, exception);
        }
    }

    @Override
    public PlaceAddressDecision getAddressDecision(
            double latitude,
            double longitude
    ) {
        try {
            return kakaoCoordinateClient.getAddress(latitude, longitude)
                    .documents()
                    .stream()
                    .findFirst()
                    .map(this::toAddressDecision)
                    .orElse(EMPTY_ADDRESS_DECISION);
        } catch (KakaoClientTimeoutException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT, exception);
        } catch (KakaoClientException exception) {
            throw new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR, exception);
        }
    }

    private PlaceAdministrativeRegion toAdministrativeRegion(
            KakaoRegionCodeResponse.Document document
    ) {
        return new PlaceAdministrativeRegion(
                normalize(document.code()),
                normalize(document.region1DepthName()),
                normalize(document.region2DepthName()),
                normalize(document.region3DepthName())
        );
    }

    private PlaceAddressDecision toAddressDecision(KakaoAddressResponse.Document document) {
        KakaoAddressResponse.Address address = document.address();
        KakaoAddressResponse.RoadAddress roadAddress = document.roadAddress();
        return new PlaceAddressDecision(
                roadAddress == null ? null : normalize(roadAddress.buildingName()),
                address == null ? null : normalize(address.addressName()),
                roadAddress == null ? null : normalize(roadAddress.addressName())
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
