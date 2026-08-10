package com.example.plimap.domain.home.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.home.dto.response.HomeResponse;
import com.example.plimap.domain.home.service.query.impl.HomeQueryServiceImpl;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HomeQueryServiceImplTest {

    private PlaceLocationMetadataService placeLocationMetadataService;
    private HomeQueryServiceImpl homeQueryService;

    @BeforeEach
    void setUp() {
        placeLocationMetadataService = mock(PlaceLocationMetadataService.class);
        homeQueryService = new HomeQueryServiceImpl(placeLocationMetadataService);
    }

    @Test
    void 닉네임과_행정구역으로_홈_컨텍스트를_조합한다() {
        // given
        when(placeLocationMetadataService.getAdministrativeRegion(37.5, 127.03))
                .thenReturn(new PlaceAdministrativeRegion(
                        "1168064000",
                        "서울특별시",
                        "강남구",
                        "역삼1동"
                ));

        // when
        HomeResponse.Context result = homeQueryService.getHomeContext(
                "델리만쥬",
                37.5,
                127.03
        );

        // then
        assertThat(result.nickname()).isEqualTo("델리만쥬");
        assertThat(result.currentRegion().sido()).isEqualTo("서울특별시");
        assertThat(result.currentRegion().sigungu()).isEqualTo("강남구");
        assertThat(result.currentRegion().eupMyeonDong()).isEqualTo("역삼1동");
        assertThat(result.currentRegion().displayName()).isEqualTo("서울특별시 강남구");
        verify(placeLocationMetadataService).getAdministrativeRegion(37.5, 127.03);
    }

    @Test
    void 닉네임은_null이어도_그대로_반환한다() {
        // given
        when(placeLocationMetadataService.getAdministrativeRegion(37.5, 127.03))
                .thenReturn(new PlaceAdministrativeRegion(null, null, null, null));

        // when
        HomeResponse.Context result = homeQueryService.getHomeContext(null, 37.5, 127.03);

        // then
        assertThat(result.nickname()).isNull();
    }

    @Test
    void sido가_없으면_displayName을_null로_반환한다() {
        // given
        when(placeLocationMetadataService.getAdministrativeRegion(37.5, 127.03))
                .thenReturn(new PlaceAdministrativeRegion(null, null, "강남구", "역삼1동"));

        // when
        HomeResponse.Context result = homeQueryService.getHomeContext("닉네임", 37.5, 127.03);

        // then
        assertThat(result.currentRegion().displayName()).isNull();
    }

    @Test
    void sigungu가_없으면_displayName을_null로_반환한다() {
        // given
        when(placeLocationMetadataService.getAdministrativeRegion(37.5, 127.03))
                .thenReturn(new PlaceAdministrativeRegion(null, "서울특별시", null, "역삼1동"));

        // when
        HomeResponse.Context result = homeQueryService.getHomeContext("닉네임", 37.5, 127.03);

        // then
        assertThat(result.currentRegion().displayName()).isNull();
    }

    @Test
    void 행정동_결과가_없어도_currentRegion_객체와_null_필드를_반환한다() {
        // given
        when(placeLocationMetadataService.getAdministrativeRegion(37.5, 127.03))
                .thenReturn(new PlaceAdministrativeRegion(null, null, null, null));

        // when
        HomeResponse.Context result = homeQueryService.getHomeContext("닉네임", 37.5, 127.03);

        // then
        assertThat(result.currentRegion()).isNotNull();
        assertThat(result.currentRegion().sido()).isNull();
        assertThat(result.currentRegion().sigungu()).isNull();
        assertThat(result.currentRegion().eupMyeonDong()).isNull();
        assertThat(result.currentRegion().displayName()).isNull();
    }

    @Test
    void Place_외부_API_오류를_그대로_전파한다() {
        // given
        PlaceException exception = new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_ERROR);
        when(placeLocationMetadataService.getAdministrativeRegion(37.5, 127.03))
                .thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> homeQueryService.getHomeContext("닉네임", 37.5, 127.03))
                .isSameAs(exception);
    }

    @Test
    void Place_외부_API_timeout을_그대로_전파한다() {
        // given
        PlaceException exception = new PlaceException(PlaceErrorCode.PLACE_EXTERNAL_API_TIMEOUT);
        when(placeLocationMetadataService.getAdministrativeRegion(37.5, 127.03))
                .thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> homeQueryService.getHomeContext("닉네임", 37.5, 127.03))
                .isSameAs(exception);
    }

    @Test
    void Member_Repository나_Service에_의존하지_않는다() {
        assertThat(HomeQueryServiceImpl.class.getDeclaredFields())
                .extracting(Field::getType)
                .allMatch(type -> !type.getPackageName().startsWith(
                        "com.example.plimap.domain.member"
                ));
    }
}
