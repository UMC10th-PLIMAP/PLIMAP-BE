package com.example.plimap.domain.place.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MapSelectionPlaceNameFormatterTest {

    @Test
    void 도로명_주소에서_국가명과_시도명을_제외한다() {
        String result = MapSelectionPlaceNameFormatter.format(
                "대한민국 서울특별시 영등포구 여의도동 123-4",
                "대한민국 서울특별시 영등포구 여의동로 123-4",
                "서울특별시"
        );

        assertThat(result).isEqualTo("영등포구 여의동로 123-4");
    }

    @Test
    void 도로명_주소가_없으면_지번_주소를_사용한다() {
        String result = MapSelectionPlaceNameFormatter.format(
                "서울특별시 영등포구 여의도동 123-4",
                null,
                "서울특별시"
        );

        assertThat(result).isEqualTo("영등포구 여의도동 123-4");
    }

    @Test
    void 도_지역은_시군구_전체_계층을_유지한다() {
        String result = MapSelectionPlaceNameFormatter.format(
                "경기도 성남시 분당구 백현동 532",
                "경기도 성남시 분당구 판교역로 166",
                "경기도"
        );

        assertThat(result).isEqualTo("성남시 분당구 판교역로 166");
    }

    @Test
    void 이미_축약된_주소는_그대로_유지한다() {
        String result = MapSelectionPlaceNameFormatter.format(
                "영등포구 여의도동 123-4",
                "영등포구 여의동로 123-4",
                "서울특별시"
        );

        assertThat(result).isEqualTo("영등포구 여의동로 123-4");
    }

    @Test
    void 축약된_시도명도_장소명에서_제외한다() {
        String result = MapSelectionPlaceNameFormatter.format(
                "경기 성남시 분당구 백현동 532",
                "경기 성남시 분당구 판교역로 166",
                "경기도"
        );

        assertThat(result).isEqualTo("성남시 분당구 판교역로 166");
    }
}
