package com.example.plimap.domain.admin.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.admin.service.command.AdminCommandService;
import com.example.plimap.domain.auth.entity.SocialAccount;
import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.auth.repository.SocialAccountRepository;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.notification.entity.Notification;
import com.example.plimap.domain.notification.enums.NotificationType;
import com.example.plimap.domain.notification.repository.NotificationRepository;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.report.entity.Report;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.repository.ReportRepository;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * report.reported_pin_id / notification.pin_id는 ON DELETE RESTRICT라, 벌점 4점 도달로
 * 핀을 하드삭제할 때 그 핀을 참조하는 row를 먼저 지우지 않으면 DB 제약 위반이 난다.
 * 이 순서를 실제 DB로 검증한다(mock 기반 단위 테스트로는 잡히지 않는 지점).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
@Transactional
class AdminPenaltyAutoWithdrawalIntegrationTest {

    @Autowired
    private AdminCommandService adminCommandService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PinRepository pinRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 벌점_4점_도달시_연관_신고_알림을_먼저_지우고_핀을_하드삭제한다() {
        // given
        Member owner = saveMember("작성자");
        ReflectionTestUtils.setField(owner, "penaltyPoint", 3);
        ReflectionTestUtils.setField(owner, "status", MemberStatus.SUSPENDED);
        entityManager.persist(SocialAccount.create(owner, AuthProvider.KAKAO, "kakao-subject", "owner@test.com"));

        Member reporter = saveMember("신고자");
        Member thirdParty = saveMember("제3자");
        Pin pin = savePin(owner);

        entityManager.persist(Report.createPinReport(reporter, pin, ReportCategory.OBSCENE_OR_HARMFUL, null));
        entityManager.persist(Notification.create(reporter, owner, pin, NotificationType.PIN_CREATED));
        // 핀과 무관한 팔로우 알림(pin_id 없음)도 탈퇴 시 함께 지워져야 한다.
        Notification followNotification = Notification.create(reporter, owner, null, NotificationType.FOLLOW);
        entityManager.persist(followNotification);
        // 탈퇴 대상 회원이 다른 회원을 신고한 이력도 함께 지워져야 한다.
        entityManager.persist(Report.createMemberReport(owner, thirdParty, ReportCategory.ABUSE_OR_HATE_SPEECH, null));
        entityManager.flush();
        entityManager.clear();

        Long pinId = pin.getId();
        Long ownerId = owner.getId();
        Long reporterId = reporter.getId();
        Long thirdPartyId = thirdParty.getId();
        Long followNotificationId = followNotification.getId();

        // when
        adminCommandService.reviewPinReport(pinId, true);
        entityManager.flush();
        entityManager.clear();

        // then
        assertThat(pinRepository.findById(pinId)).isEmpty();
        assertThat(reportRepository.existsByReporter_IdAndReportedPin_Id(reporterId, pinId)).isFalse();
        assertThat(reportRepository.existsByReporter_IdAndReportedMember_Id(ownerId, thirdPartyId)).isFalse();
        assertThat(notificationRepository.findById(followNotificationId)).isEmpty();

        Member withdrawn = memberRepository.findById(ownerId).orElseThrow();
        assertThat(withdrawn.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        assertThat(withdrawn.getPenaltyPoint()).isEqualTo(4);
        assertThat(withdrawn.isDeleted()).isTrue();

        // SocialAccount는 재가입 영구 차단의 근거라 유지되어야 한다.
        assertThat(socialAccountRepository.findByProviderAndProviderSubject(AuthProvider.KAKAO, "kakao-subject"))
                .isPresent();
    }

    private Member saveMember(String nickname) {
        Member member = Member.builder()
                .nickname(nickname)
                .name(nickname)
                .build();
        entityManager.persist(member);
        entityManager.flush();
        return member;
    }

    private Pin savePin(Member author) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Place place = Place.createMapSelection(
                "테스트 장소",
                "서울특별시 테스트구",
                null,
                geometryFactory.createPoint(new Coordinate(127.0, 37.0))
        );
        Track track = Track.create(
                "YOUTUBE",
                "admin-penalty-test-track",
                "테스트 곡",
                "테스트 가수",
                null,
                null,
                null,
                null
        );
        PlaceTrack placeTrack = PlaceTrack.create(place, track);
        Pin pin = Pin.builder()
                .member(author)
                .place(place)
                .placeTrack(placeTrack)
                .clipStartMs(0)
                .introduction("테스트 PIN")
                .isFeedPublic(true)
                .build();

        entityManager.persist(place);
        entityManager.persist(track);
        entityManager.persist(placeTrack);
        entityManager.persist(pin);
        entityManager.flush();
        return pin;
    }
}
