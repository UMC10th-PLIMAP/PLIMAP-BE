package com.example.plimap.domain.place.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.global.external.kakao.KakaoPlaceSearchClient;
import com.example.plimap.global.external.kakao.dto.KakaoPlaceSearchResponse;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class PlaceSearchPersistenceIntegrationTest {

    @Autowired
    private PlaceQueryService placeQueryService;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private KakaoPlaceSearchClient kakaoPlaceSearchClient;

    @MockitoBean
    private PinQueryService pinQueryService;

    @Test
    void 장소_검색_전후_Place와_검색_이력_수가_변하지_않는다() {
        long placeCountBefore = count("place");
        long searchHistoryCountBefore = count("place_search_history");
        when(kakaoPlaceSearchClient.search("한강", 37.5283, 126.9326))
                .thenReturn(new KakaoPlaceSearchResponse(List.of(
                        new KakaoPlaceSearchResponse.Document(
                                "26338954",
                                "한강",
                                "여행 > 관광,명소 > 공원",
                                "서울특별시 영등포구 여의도동",
                                "서울특별시 영등포구 여의동로",
                                "126.9326",
                                "37.5283",
                                "470"
                        )
                )));

        placeQueryService.searchPlaces(new PlaceRequest.Search(
                "한강",
                37.5283,
                126.9326
        ));
        entityManager.flush();
        entityManager.clear();

        assertThat(count("place")).isEqualTo(placeCountBefore);
        assertThat(count("place_search_history")).isEqualTo(searchHistoryCountBefore);
    }

    private long count(String tableName) {
        return ((Number) entityManager.createNativeQuery(
                "SELECT COUNT(*) FROM " + tableName
        ).getSingleResult()).longValue();
    }
}
