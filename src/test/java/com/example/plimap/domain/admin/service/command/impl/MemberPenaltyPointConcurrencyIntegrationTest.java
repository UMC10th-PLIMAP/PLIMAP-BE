package com.example.plimap.domain.admin.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.admin.service.command.AdminCommandService;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.repository.TrackRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 동일 회원에 대한 벌점 부여 요청이 동시에 들어와도 penaltyPoint 증가가 유실되지 않는지
 * 실제 DB로 검증한다(PESSIMISTIC_WRITE 잠금 없이는 재현되는 lost-update 시나리오).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
class MemberPenaltyPointConcurrencyIntegrationTest {

    private static final int PIN_COUNT = 3;
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Autowired
    private AdminCommandService adminCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private PlaceTrackRepository placeTrackRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long ownerId;
    private final List<Long> pinIds = new ArrayList<>();

    @AfterEach
    void tearDown() {
        for (Long pinId : pinIds) {
            jdbcTemplate.update("DELETE FROM pin WHERE id = ?", pinId);
        }
        if (ownerId != null) {
            jdbcTemplate.update("DELETE FROM member WHERE id = ?", ownerId);
        }
    }

    @Test
    void 같은_회원에_대한_동시_벌점_부여_요청은_증가분이_유실되지_않는다() throws Exception {
        Member owner = memberRepository.save(Member.builder().nickname("동시탈퇴대상").build());
        ownerId = owner.getId();
        for (int i = 0; i < PIN_COUNT; i++) {
            pinIds.add(savePin(owner, i).getId());
        }

        ExecutorService executor = Executors.newFixedThreadPool(PIN_COUNT);
        CountDownLatch ready = new CountDownLatch(PIN_COUNT);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Long pinId : pinIds) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    adminCommandService.reviewPinReport(pinId, true);
                    return null;
                }));
            }
            ready.await();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        Member reloaded = memberRepository.findById(ownerId).orElseThrow();
        assertThat(reloaded.getPenaltyPoint()).isEqualTo(PIN_COUNT);
        assertThat(reloaded.getStatus()).isEqualTo(MemberStatus.SUSPENDED);
    }

    private Pin savePin(Member author, int index) {
        Place place = placeRepository.save(Place.createMapSelection(
                "동시성 테스트 장소" + index,
                "서울특별시 테스트구",
                null,
                GEOMETRY_FACTORY.createPoint(new Coordinate(127.0 + index * 0.001, 37.0))
        ));
        Track track = trackRepository.save(Track.create(
                "YOUTUBE",
                "concurrency-test-track-" + index,
                "테스트 곡" + index,
                "테스트 가수",
                null,
                null,
                null,
                null
        ));
        PlaceTrack placeTrack = placeTrackRepository.save(PlaceTrack.create(place, track));
        Pin pin = Pin.builder()
                .member(author)
                .place(place)
                .placeTrack(placeTrack)
                .clipStartMs(0)
                .introduction("동시성 테스트 PIN" + index)
                .isFeedPublic(true)
                .build();

        return pinRepository.save(pin);
    }
}
