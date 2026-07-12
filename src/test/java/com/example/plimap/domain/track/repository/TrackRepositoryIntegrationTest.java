package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class TrackRepositoryIntegrationTest {

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private PlaceTrackRepository placeTrackRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void Track을_식별키로_조회하고_중복_저장을_방지한다() {
        Track savedTrack = trackRepository.saveAndFlush(track("video-id"));

        Track foundTrack = trackRepository
                .findByProviderAndProviderTrackId("YOUTUBE", "video-id")
                .orElseThrow();

        assertThat(foundTrack.getId()).isEqualTo(savedTrack.getId());
        assertThatThrownBy(() -> trackRepository.saveAndFlush(track("video-id")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 활성_PlaceTrack을_장소와_트랙으로_조회한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("active-video-id"));
        PlaceTrack savedPlaceTrack = placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));

        PlaceTrack foundPlaceTrack = placeTrackRepository
                .findByPlace_IdAndTrack_IdAndDeletedAtIsNull(place.getId(), track.getId())
                .orElseThrow();

        assertThat(foundPlaceTrack.getId()).isEqualTo(savedPlaceTrack.getId());
        assertThat(foundPlaceTrack.getDeletedAt()).isNull();
    }

    @Test
    void 삭제된_PlaceTrack을_조회하고_복구하면_활성_조회가_가능하다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("deleted-video-id"));
        PlaceTrack placeTrack = placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));

        placeTrack.delete();
        entityManager.flush();
        entityManager.clear();

        PlaceTrack deletedPlaceTrack = placeTrackRepository
                .findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(
                        place.getId(),
                        track.getId()
                )
                .orElseThrow();

        deletedPlaceTrack.restore();
        entityManager.flush();
        entityManager.clear();

        PlaceTrack restoredPlaceTrack = placeTrackRepository
                .findByPlace_IdAndTrack_IdAndDeletedAtIsNull(place.getId(), track.getId())
                .orElseThrow();

        assertThat(restoredPlaceTrack.getId()).isEqualTo(placeTrack.getId());
        assertThat(restoredPlaceTrack.getDeletedAt()).isNull();
    }

    private Track track(String providerTrackId) {
        return Track.create(
                "YOUTUBE",
                providerTrackId,
                "title",
                "artist",
                "album",
                "https://example.com/album.jpg",
                "https://example.com/preview",
                180_000
        );
    }

    private Place savePlace() {
        Point location = new GeometryFactory(new PrecisionModel(), 4326)
                .createPoint(new Coordinate(127.0, 37.0));

        Place place = Place.builder()
                .name("테스트 장소")
                .address("서울특별시 테스트구")
                .source(PlaceSource.MAP_SELECTION)
                .location(location)
                .build();

        entityManager.persist(place);
        entityManager.flush();
        return place;
    }
}
