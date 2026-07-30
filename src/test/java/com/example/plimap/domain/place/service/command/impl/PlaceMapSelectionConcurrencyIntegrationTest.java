package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import com.example.plimap.support.PostgisContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
class PlaceMapSelectionConcurrencyIntegrationTest {

    private static final int REQUEST_COUNT = 8;

    @Autowired
    private PlaceCommandServiceImpl placeCommandService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlacePersistenceService placePersistenceService;

    @MockitoBean
    private PlaceLocationMetadataService placeLocationMetadataService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM place");
        when(placeLocationMetadataService.getAdministrativeRegion(37.5283, 126.9326))
                .thenReturn(new PlaceAdministrativeRegion(null, null, null, null));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM place");
    }

    @Test
    void 동시_요청에서도_MAP_SELECTION_장소를_하나만_생성한다() throws Exception {
        assertThat(AopUtils.isAopProxy(placePersistenceService)).isTrue();

        PlaceRequest.MapSelection request = new PlaceRequest.MapSelection(
                37.5283,
                126.9326,
                "물빛무대 앞 광장",
                "서울특별시 영등포구 여의도동",
                "서울특별시 영등포구 여의동로"
        );
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);

        try {
            List<Future<PlaceResponse.MapSelection>> futures = new ArrayList<>();
            for (int index = 0; index < REQUEST_COUNT; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return placeCommandService.confirmMapSelection(request);
                }));
            }

            start.countDown();

            List<Long> placeIds = new ArrayList<>();
            for (Future<PlaceResponse.MapSelection> future : futures) {
                placeIds.add(future.get(30, TimeUnit.SECONDS).placeId());
            }

            Long activeMapSelectionCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM place
                    WHERE source = 'MAP_SELECTION'
                      AND deleted_at IS NULL
                    """, Long.class);
            assertThat(placeIds).containsOnly(placeIds.getFirst());
            assertThat(activeMapSelectionCount).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }
}
