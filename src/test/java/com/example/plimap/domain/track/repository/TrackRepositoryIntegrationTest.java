package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.PlaceTrackLike;
import com.example.plimap.domain.track.entity.PlaceTrackLikeId;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
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
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
@Transactional
class TrackRepositoryIntegrationTest {

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private PlaceTrackRepository placeTrackRepository;

    @Autowired
    private PlaceTrackLikeRepository placeTrackLikeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void Track을_식별키로_조회하고_중복_저장을_방지한다() {
        Track savedTrack = trackRepository.saveAndFlush(track("youtube-video-id"));

        Track foundTrack = trackRepository
                .findByProviderAndProviderTrackId("YOUTUBE", "youtube-video-id")
                .orElseThrow();

        assertThat(foundTrack.getId()).isEqualTo(savedTrack.getId());
        assertThatThrownBy(() -> trackRepository.saveAndFlush(track("youtube-video-id")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 활성_PlaceTrack을_장소와_트랙으로_조회한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("active-youtube-video-id"));
        PlaceTrack savedPlaceTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));

        PlaceTrack foundPlaceTrack = placeTrackRepository
                .findByPlace_IdAndTrack_IdAndDeletedAtIsNull(place.getId(), track.getId())
                .orElseThrow();

        assertThat(foundPlaceTrack.getId()).isEqualTo(savedPlaceTrack.getId());
        assertThat(foundPlaceTrack.getDeletedAt()).isNull();
    }

    @Test
    void 장소_노래_상세는_활성_PlaceTrack과_Track을_함께_조회한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("detail-youtube-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));
        entityManager.clear();

        PlaceTrack foundPlaceTrack = placeTrackRepository
                .findDetailByIdAndDeletedAtIsNull(placeTrack.getId())
                .orElseThrow();

        assertThat(foundPlaceTrack.getId()).isEqualTo(placeTrack.getId());
        assertThat(foundPlaceTrack.getTrack().getId()).isEqualTo(track.getId());
        assertThat(foundPlaceTrack.getTrack().getProviderTrackId())
                .isEqualTo("detail-youtube-video-id");
    }

    @Test
    void 삭제된_PlaceTrack은_상세_조회에서_제외한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("deleted-detail-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));
        placeTrack.delete();
        entityManager.flush();
        entityManager.clear();

        assertThat(placeTrackRepository
                .findDetailByIdAndDeletedAtIsNull(placeTrack.getId()))
                .isEmpty();
    }

    @Test
    void 삭제된_Place의_활성_PlaceTrack은_상세_조회에서_제외한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("deleted-place-detail-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));
        place.delete();
        entityManager.flush();
        entityManager.clear();

        assertThat(placeTrackRepository
                .findDetailByIdAndDeletedAtIsNull(placeTrack.getId()))
                .isEmpty();
    }

    @Test
    void 사용자별_PlaceTrack_좋아요_여부를_정확히_조회한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("like-detail-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));
        Member member = saveMember();
        placeTrackLikeRepository.saveAndFlush(
                PlaceTrackLike.create(placeTrack, member.getId())
        );

        assertThat(placeTrackLikeRepository.existsById(
                new PlaceTrackLikeId(placeTrack.getId(), member.getId())
        )).isTrue();
        assertThat(placeTrackLikeRepository.existsById(
                new PlaceTrackLikeId(placeTrack.getId(), member.getId() + 1L)
        )).isFalse();
    }

    @Test
    void 좋아요_변경용_조회는_활성_Place와_PlaceTrack을_반환한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("active-like-target-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));

        PlaceTrack foundPlaceTrack = placeTrackRepository
                .findActiveByIdForUpdate(placeTrack.getId())
                .orElseThrow();

        assertThat(foundPlaceTrack.getId()).isEqualTo(placeTrack.getId());
        assertThat(foundPlaceTrack.getDeletedAt()).isNull();
        assertThat(foundPlaceTrack.getPlace().getDeletedAt()).isNull();
    }

    @Test
    void 좋아요_변경용_조회는_삭제된_PlaceTrack을_제외한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("deleted-like-target-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));
        placeTrack.delete();
        entityManager.flush();
        entityManager.clear();

        assertThat(placeTrackRepository
                .findActiveByIdForUpdate(placeTrack.getId()))
                .isEmpty();
    }

    @Test
    void 좋아요_변경용_조회는_삭제된_Place의_활성_PlaceTrack을_제외한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("deleted-place-like-target-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));
        place.delete();
        entityManager.flush();
        entityManager.clear();

        assertThat(placeTrackRepository
                .findActiveByIdForUpdate(placeTrack.getId()))
                .isEmpty();
    }

    @Test
    void 현재_사용자의_PlaceTrackLike만_조회하고_삭제한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("member-like-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));
        Member member = saveMember("좋아요사용자1");
        Member otherMember = saveMember("다른사용자1");
        placeTrackLikeRepository.saveAndFlush(
                PlaceTrackLike.create(placeTrack, member.getId())
        );
        placeTrackLikeRepository.saveAndFlush(
                PlaceTrackLike.create(placeTrack, otherMember.getId())
        );

        PlaceTrackLike foundLike = placeTrackLikeRepository.findById(
                new PlaceTrackLikeId(placeTrack.getId(), member.getId())
        ).orElseThrow();
        placeTrackLikeRepository.delete(foundLike);
        placeTrackLikeRepository.flush();
        entityManager.clear();

        assertThat(placeTrackLikeRepository.existsById(
                new PlaceTrackLikeId(placeTrack.getId(), member.getId())
        )).isFalse();
        assertThat(placeTrackLikeRepository.existsById(
                new PlaceTrackLikeId(placeTrack.getId(), otherMember.getId())
        )).isTrue();
    }

    @Test
    void 삭제된_PlaceTrack을_조회하고_복구하면_활성_조회가_가능하다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("deleted-youtube-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));

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

    @Test
    void PlaceTrackLike를_정적_팩터리_메서드로_생성한다() {
        Place place = savePlace();
        Track track = trackRepository.save(track("liked-youtube-video-id"));
        PlaceTrack placeTrack =
                placeTrackRepository.saveAndFlush(PlaceTrack.create(place, track));

        PlaceTrackLike placeTrackLike = PlaceTrackLike.create(placeTrack, 1L);

        assertThat(placeTrackLike.getPlaceTrack()).isEqualTo(placeTrack);
        assertThat(placeTrackLike.getId().getPlaceTrackId()).isEqualTo(placeTrack.getId());
        assertThat(placeTrackLike.getId().getMemberId()).isEqualTo(1L);
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

    private Member saveMember() {
        return saveMember("좋아요사용자");
    }

    private Member saveMember(String nickname) {
        Member member = Member.builder()
                .nickname(nickname)
                .build();
        entityManager.persist(member);
        entityManager.flush();
        return member;
    }
}
