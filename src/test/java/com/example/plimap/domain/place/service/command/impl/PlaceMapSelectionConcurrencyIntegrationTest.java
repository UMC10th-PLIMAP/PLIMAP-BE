package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.place.dto.PlaceAddressDecision;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.dto.request.PlaceRequest;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.enums.MapSelectionStatus;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import com.example.plimap.support.PostgisContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
        when(placeLocationMetadataService.getAddressDecision(37.5283, 126.9326))
                .thenReturn(new PlaceAddressDecision(null, null, null));
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
                "서울특별시 영등포구 여의도동 123-4",
                "서울특별시 영등포구 여의동로 123-4"
        );
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);

        try {
            List<Future<PlaceResponse.MapSelectionResult>> futures = new ArrayList<>();
            for (int index = 0; index < REQUEST_COUNT; index++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return placeCommandService.confirmMapSelection(request);
                }));
            }

            start.countDown();

            List<Long> placeIds = new ArrayList<>();
            for (Future<PlaceResponse.MapSelectionResult> future : futures) {
                placeIds.add(future.get(30, TimeUnit.SECONDS).mapSelection().placeId());
            }

            Long activeMapSelectionCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM place
                    WHERE source = 'MAP_SELECTION'
                      AND deleted_at IS NULL
                    """, Long.class);
            assertThat(placeIds).containsOnly(placeIds.getFirst());
            assertThat(activeMapSelectionCount).isEqualTo(1L);
            assertThat(jdbcTemplate.queryForObject("""
                    SELECT name
                    FROM place
                    WHERE id = ?
                    """, String.class, placeIds.getFirst()))
                    .isEqualTo("영등포구 여의동로 123-4");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void 기존_MAP_SELECTION을_재사용하면_저장된_장문_장소명도_축약한다() {
        Long placeId = jdbcTemplate.queryForObject("""
                INSERT INTO place (
                    name,
                    address,
                    road_address,
                    sido,
                    source,
                    location,
                    created_at,
                    updated_at
                )
                VALUES (
                    '대한민국 서울특별시 영등포구 여의동로 123-4',
                    '대한민국 서울특별시 영등포구 여의도동 123-4',
                    '대한민국 서울특별시 영등포구 여의동로 123-4',
                    '서울특별시',
                    'MAP_SELECTION',
                    ST_SetSRID(ST_MakePoint(126.9326, 37.5283), 4326)::geography,
                    CURRENT_TIMESTAMP,
                    CURRENT_TIMESTAMP
                )
                RETURNING id
                """, Long.class);

        PlaceResponse.MapSelectionResult result = placeCommandService.confirmMapSelection(
                new PlaceRequest.MapSelection(
                        37.5283,
                        126.9326,
                        "서울특별시 영등포구 여의도동 123-4",
                        "서울특별시 영등포구 여의동로 123-4"
                )
        );

        assertThat(result.mapSelection().placeId()).isEqualTo(placeId);
        assertThat(result.mapSelection().placeName()).isEqualTo("영등포구 여의동로 123-4");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM place WHERE id = ?",
                String.class,
                placeId
        )).isEqualTo("영등포구 여의동로 123-4");
    }

    @Test
    void coord2address_호출_중에는_DB_트랜잭션을_점유하지_않는다() {
        AtomicBoolean transactionActiveDuringCall = new AtomicBoolean(true);
        when(placeLocationMetadataService.getAddressDecision(37.5283, 126.9326))
                .thenAnswer(invocation -> {
                    transactionActiveDuringCall.set(
                            TransactionSynchronizationManager.isActualTransactionActive()
                    );
                    return new PlaceAddressDecision(
                            "카카오 판교아지트",
                            "서울특별시 영등포구 여의도동",
                            "서울특별시 영등포구 여의동로"
                    );
                });

        PlaceResponse.MapSelectionResult result = placeCommandService.confirmMapSelection(
                new PlaceRequest.MapSelection(
                        37.5283,
                        126.9326,
                        "서울특별시 영등포구 여의도동 123-4",
                        "서울특별시 영등포구 여의동로 123-4"
                )
        );

        assertThat(transactionActiveDuringCall).isFalse();
        assertThat(result.status()).isEqualTo(MapSelectionStatus.PLACE_SEARCH_REQUIRED);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM place",
                Long.class
        )).isZero();
    }
}
