package com.example.plimap.domain.place.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceBookmarkId;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.repository.PlaceBookmarkRepository;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
class PlaceBookmarkConcurrencyIntegrationTest {

    private static final int REQUEST_COUNT = 8;
    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private PlaceCommandService placeCommandService;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private PlaceBookmarkRepository placeBookmarkRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void 동시_등록에도_동일_회원과_장소의_북마크는_하나만_생성된다() throws Exception {
        Member member = memberRepository.save(Member.builder()
                .nickname("동시회원")
                .name("테스터")
                .build());
        Place place = placeRepository.save(place());
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        CountDownLatch ready = new CountDownLatch(REQUEST_COUNT);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < REQUEST_COUNT; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return placeCommandService.bookmarkPlace(member.getId(), place.getId());
                }));
            }
            ready.await();
            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(placeBookmarkRepository.findAllById(List.of(
                new PlaceBookmarkId(place.getId(), member.getId())
        ))).hasSize(1);
    }

    private Place place() {
        Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(126.9326, 37.5283));
        return Place.builder()
                .name("동시등록장소")
                .address("서울특별시 영등포구 여의도동")
                .roadAddress("서울특별시 영등포구 여의동로")
                .source(PlaceSource.MAP_SELECTION)
                .location(location)
                .build();
    }
}
