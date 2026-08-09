package com.example.plimap.domain.place.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.service.query.impl.PlaceLocationMetadataServiceImpl;
import com.example.plimap.global.external.kakao.KakaoClientException;
import com.example.plimap.global.external.kakao.KakaoClientTimeoutException;
import com.example.plimap.global.external.kakao.KakaoCoordinateClient;
import com.example.plimap.global.external.kakao.dto.KakaoAddressResponse;
import com.example.plimap.global.external.kakao.dto.KakaoRegionCodeResponse;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlaceLocationMetadataServiceImplTest {

    private final KakaoCoordinateClient kakaoCoordinateClient =
            mock(KakaoCoordinateClient.class);
    private PlaceLocationMetadataServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PlaceLocationMetadataServiceImpl(kakaoCoordinateClient);
    }

    @Test
    void H_타입_행정구역만_선택하고_누락_계층은_null로_정규화한다() {
        when(kakaoCoordinateClient.getRegionCodes(37.5283, 126.9326))
                .thenReturn(new KakaoRegionCodeResponse(List.of(
                        new KakaoRegionCodeResponse.Document(
                                "B",
                                "1156011000",
                                "서울특별시",
                                "영등포구",
                                "여의도동"
                        ),
                        new KakaoRegionCodeResponse.Document(
                                "H",
                                " 1156054000 ",
                                " 서울특별시 ",
                                "   ",
                                null
                        )
                )));

        var region = service.getAdministrativeRegion(37.5283, 126.9326);

        assertThat(region.code()).isEqualTo("1156054000");
        assertThat(region.sido()).isEqualTo("서울특별시");
        assertThat(region.sigungu()).isNull();
        assertThat(region.eupMyeonDong()).isNull();
    }

    @Test
    void H_타입이_없으면_모든_행정구역_값을_null로_반환한다() {
        when(kakaoCoordinateClient.getRegionCodes(37.5283, 126.9326))
                .thenReturn(new KakaoRegionCodeResponse(List.of(
                        new KakaoRegionCodeResponse.Document(
                                "B",
                                "1156011000",
                                "서울특별시",
                                "영등포구",
                                "여의도동"
                        )
                )));

        var region = service.getAdministrativeRegion(37.5283, 126.9326);

        assertThat(region.code()).isNull();
        assertThat(region.sido()).isNull();
        assertThat(region.sigungu()).isNull();
        assertThat(region.eupMyeonDong()).isNull();
    }

    @Test
    void documents가_비어_있으면_모든_행정구역_값을_null로_반환한다() {
        when(kakaoCoordinateClient.getRegionCodes(37.5283, 126.9326))
                .thenReturn(new KakaoRegionCodeResponse(List.of()));

        var region = service.getAdministrativeRegion(37.5283, 126.9326);

        assertThat(region.code()).isNull();
        assertThat(region.sido()).isNull();
        assertThat(region.sigungu()).isNull();
        assertThat(region.eupMyeonDong()).isNull();
    }

    @Test
    void coord2address로_건물명과_지번_도로명_주소를_판정한다() {
        when(kakaoCoordinateClient.getAddress(37.5283, 126.9326))
                .thenReturn(new KakaoAddressResponse(List.of(
                        new KakaoAddressResponse.Document(
                                new KakaoAddressResponse.Address(" 서울 영등포구 여의도동 "),
                                new KakaoAddressResponse.RoadAddress(
                                        " 서울 영등포구 여의동로 330 ",
                                        " 물빛무대 "
                                )
                        )
                )));

        var decision = service.getAddressDecision(37.5283, 126.9326);

        assertThat(decision.hasBuildingName()).isTrue();
        assertThat(decision.buildingName()).isEqualTo("물빛무대");
        assertThat(decision.address()).isEqualTo("서울 영등포구 여의도동");
        assertThat(decision.roadAddress()).isEqualTo("서울 영등포구 여의동로 330");
    }

    @Test
    void 행정구역_API_오류를_Place_외부_API_오류로_변환한다() {
        when(kakaoCoordinateClient.getRegionCodes(37.5283, 126.9326))
                .thenThrow(new KakaoClientException("failed"));

        assertThatThrownBy(() -> service.getAdministrativeRegion(37.5283, 126.9326))
                .isInstanceOf(PlaceException.class)
                .extracting(exception -> ((PlaceException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
    }

    @Test
    void 주소_API_timeout을_Place_외부_API_timeout으로_변환한다() {
        when(kakaoCoordinateClient.getAddress(37.5283, 126.9326))
                .thenThrow(new KakaoClientTimeoutException(
                        "timeout",
                        new SocketTimeoutException()
                ));

        assertThatThrownBy(() -> service.getAddressDecision(37.5283, 126.9326))
                .isInstanceOf(PlaceException.class)
                .extracting(exception -> ((PlaceException) exception).getErrorCode())
                .isEqualTo(PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT);
    }
}
