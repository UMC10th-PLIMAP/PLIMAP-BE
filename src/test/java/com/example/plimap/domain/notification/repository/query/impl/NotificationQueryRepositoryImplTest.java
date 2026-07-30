package com.example.plimap.domain.notification.repository.query.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResDTO;
import com.example.plimap.domain.notification.entity.Notification;
import com.example.plimap.domain.notification.enums.NotificationType;
import com.example.plimap.domain.notification.exception.NotificationErrorCode;
import com.example.plimap.domain.notification.exception.NotificationException;
import com.example.plimap.domain.notification.repository.NotificationRepository;
import com.example.plimap.domain.notification.repository.query.NotificationQueryRepository;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.domain.track.repository.PlaceTrackRepository;
import com.example.plimap.domain.track.repository.TrackRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgisContainerConfiguration.class)
@Transactional
class NotificationQueryRepositoryImplTest {

    @Autowired
    private NotificationQueryRepository notificationQueryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private PlaceTrackRepository placeTrackRepository;

    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private EntityManager entityManager;

    private Member recipient;
    private Member actor;
    private Notification followNotification;
    private Notification pinLikedNotification;
    private Notification pinCreatedNotification;

    @BeforeEach
    void setup() {
        recipient = memberRepository.save(createMember("받는이", "받는이닉네임"));
        actor = memberRepository.save(createMember("보낸이", "보낸이닉네임"));

        Place place = placeRepository.save(Place.builder()
                .name("여의도 한강공원")
                .address("서울 영등포구 여의동로 330")
                .source(PlaceSource.MAP_SELECTION)
                .location(new GeometryFactory(new PrecisionModel(), 4326)
                        .createPoint(new Coordinate(126.9326, 37.5283)))
                .build());

        Track track = trackRepository.save(Track.create(
                "provider", "providerTrackId", "title", "artist", null, "album", "url_test", null
        ));

        PlaceTrack placeTrack = placeTrackRepository.save(PlaceTrack.create(place, track));

        Pin pin = pinRepository.save(Pin.builder()
                .member(actor)
                .place(place)
                .placeTrack(placeTrack)
                .clipStartMs(70000)
                .introduction("좋아요")
                .isFeedPublic(true)
                .build());

        followNotification = notificationRepository.save(
                Notification.create(recipient, actor, null, NotificationType.FOLLOW));
        pinLikedNotification = notificationRepository.save(
                Notification.create(recipient, actor, pin, NotificationType.PIN_LIKED));
        pinCreatedNotification = notificationRepository.save(
                Notification.create(recipient, actor, pin, NotificationType.PIN_CREATED));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 알림_목록을_최신순_커서_페이지네이션으로_조회한다() {
        // when
        Pagination<NotificationResDTO.Item> firstPage =
                notificationQueryRepository.findNotifications(recipient.getId(), null, 2);

        // then
        assertThat(firstPage.data()).hasSize(2);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(firstPage.data())
                .extracting(NotificationResDTO.Item::notificationId)
                .containsExactly(pinCreatedNotification.getId(), pinLikedNotification.getId());

        // when
        Pagination<NotificationResDTO.Item> secondPage =
                notificationQueryRepository.findNotifications(recipient.getId(), firstPage.nextCursor(), 2);

        // then
        assertThat(secondPage.data()).hasSize(1);
        assertThat(secondPage.hasNext()).isFalse();
        assertThat(secondPage.nextCursor()).isNull();
        assertThat(secondPage.data())
                .extracting(NotificationResDTO.Item::notificationId)
                .containsExactly(followNotification.getId());
    }

    @Test
    void 알림에는_알림_유형과_대상_핀_존재_여부가_반영된다() {
        // when
        Pagination<NotificationResDTO.Item> page =
                notificationQueryRepository.findNotifications(recipient.getId(), null, 10);

        // then
        assertThat(page.data())
                .filteredOn(item -> item.type() == NotificationType.FOLLOW)
                .extracting(NotificationResDTO.Item::pinId)
                .containsExactly((Long) null);
        assertThat(page.data())
                .filteredOn(item -> item.type() == NotificationType.PIN_LIKED)
                .extracting(NotificationResDTO.Item::pinId)
                .doesNotContainNull();
    }

    @Test
    void 뒤에_빈_구간이_붙은_잘못된_커서는_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() ->
                notificationQueryRepository.findNotifications(recipient.getId(), "2026-01-01T00:00:00Z/1/", 10))
                .isInstanceOfSatisfying(NotificationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(NotificationErrorCode.INVALID_CURSOR));
    }

    @Test
    void id가_0_이하인_커서는_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() ->
                notificationQueryRepository.findNotifications(recipient.getId(), "2026-01-01T00:00:00Z/0", 10))
                .isInstanceOfSatisfying(NotificationException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(NotificationErrorCode.INVALID_CURSOR));
    }

    @Test
    void 다른_회원에게_온_알림은_노출되지_않는다() {
        // when
        Pagination<NotificationResDTO.Item> page =
                notificationQueryRepository.findNotifications(actor.getId(), null, 10);

        // then
        assertThat(page.data()).isEmpty();
        assertThat(page.hasNext()).isFalse();
    }

    private Member createMember(String name, String nickname) {
        return Member.builder()
                .name(name)
                .nickname(nickname)
                .introduction("안녕하세요")
                .profileImageObjectKey("image_url")
                .build();
    }
}
