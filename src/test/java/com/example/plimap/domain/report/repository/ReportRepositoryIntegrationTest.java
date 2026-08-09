package com.example.plimap.domain.report.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.report.dto.ReportReason;
import com.example.plimap.domain.report.entity.Report;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.entity.Track;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
@Transactional
class ReportRepositoryIntegrationTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 회원과_PIN_신고를_저장하고_대상별로_존재_여부를_조회한다() {
        // given
        Member reporter = saveMember("신고자");
        Member reportedMember = saveMember("신고대상");
        Member pinAuthor = saveMember("핀작성자");
        Pin reportedPin = savePin(pinAuthor);

        Report memberReport = Report.createMemberReport(
                reporter,
                reportedMember,
                ReportCategory.PERSONAL_INFORMATION_EXPOSURE,
                null
        );
        Report pinReport = Report.createPinReport(
                reporter,
                reportedPin,
                ReportCategory.OTHER,
                "가"
        );

        // when
        reportRepository.saveAllAndFlush(List.of(memberReport, pinReport));
        entityManager.clear();

        // then
        assertThat(reportRepository.existsByReporter_IdAndReportedMember_Id(
                reporter.getId(),
                reportedMember.getId()
        )).isTrue();
        assertThat(reportRepository.existsByReporter_IdAndReportedPin_Id(
                reporter.getId(),
                reportedPin.getId()
        )).isTrue();
    }

    @Test
    void 동일한_회원에_대한_중복_신고를_저장할_수_없다() {
        // given
        Member reporter = saveMember("신고자");
        Member reportedMember = saveMember("신고대상");
        reportRepository.saveAndFlush(Report.createMemberReport(
                reporter,
                reportedMember,
                ReportCategory.OBSCENE_OR_HARMFUL,
                null
        ));

        Report duplicate = Report.createMemberReport(
                reporter,
                reportedMember,
                ReportCategory.ABUSE_OR_HATE_SPEECH,
                null
        );

        // when & then
        assertThatThrownBy(() -> reportRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 동일한_PIN에_대한_중복_신고를_저장할_수_없다() {
        // given
        Member reporter = saveMember("신고자");
        Pin reportedPin = savePin(saveMember("핀작성자"));
        reportRepository.saveAndFlush(Report.createPinReport(
                reporter,
                reportedPin,
                ReportCategory.OBSCENE_OR_HARMFUL,
                null
        ));

        Report duplicate = Report.createPinReport(
                reporter,
                reportedPin,
                ReportCategory.COMMERCIAL_OR_PROMOTIONAL,
                null
        );

        // when & then
        assertThatThrownBy(() -> reportRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void DB에서도_회원과_PIN_중_하나의_신고_대상만_허용한다() {
        // given
        Member reporter = saveMember("신고자");

        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO report (reporter_member_id, category)
                VALUES (?, 'OBSCENE_OR_HARMFUL')
                """, reporter.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void DB에서도_자기_자신에_대한_회원_신고를_허용하지_않는다() {
        // given
        Member reporter = saveMember("신고자");

        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO report (reporter_member_id, reported_member_id, category)
                VALUES (?, ?, 'OBSCENE_OR_HARMFUL')
                """, reporter.getId(), reporter.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void DB에서도_기타_신고의_공백_내용을_허용하지_않는다() {
        // given
        Member reporter = saveMember("신고자");
        Member reportedMember = saveMember("신고대상");

        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO report (reporter_member_id, reported_member_id, category, detail)
                VALUES (?, ?, 'OTHER', ' ')
                """, reporter.getId(), reportedMember.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findReasonsByReportedPinIds는_핀별_신고_사유를_최신순으로_반환한다() {
        // given
        Member reporter1 = saveMember("신고자1");
        Member reporter2 = saveMember("신고자2");
        Pin targetPin = savePin(saveMember("핀작성자"));
        Pin otherPin = savePin(saveMember("다른핀작성자"));

        reportRepository.saveAndFlush(
                Report.createPinReport(reporter1, targetPin, ReportCategory.OBSCENE_OR_HARMFUL, null));
        reportRepository.saveAndFlush(
                Report.createPinReport(reporter2, targetPin, ReportCategory.OTHER, "상세"));
        reportRepository.saveAndFlush(
                Report.createPinReport(reporter1, otherPin, ReportCategory.COMMERCIAL_OR_PROMOTIONAL, null));
        entityManager.clear();

        // when
        List<ReportReason> result = reportRepository.findReasonsByReportedPinIds(List.of(targetPin.getId()));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(ReportReason::pinId).containsOnly(targetPin.getId());
        assertThat(result).extracting(ReportReason::category)
                .containsExactlyInAnyOrder(ReportCategory.OBSCENE_OR_HARMFUL, ReportCategory.OTHER);
    }

    @Test
    void 반려_후_재신고하면_반려된_사유는_제외하고_새_신고_사유만_조회된다() {
        // given: 신고 2건이 쌓인 핀을 관리자가 반려(markReviewedByReportedPinId) 처리
        Member reporter1 = saveMember("신고자1");
        Member reporter2 = saveMember("신고자2");
        Pin targetPin = savePin(saveMember("핀작성자"));

        reportRepository.saveAndFlush(
                Report.createPinReport(reporter1, targetPin, ReportCategory.OBSCENE_OR_HARMFUL, null));
        reportRepository.saveAndFlush(
                Report.createPinReport(reporter2, targetPin, ReportCategory.ABUSE_OR_HATE_SPEECH, null));
        entityManager.flush();

        reportRepository.markReviewedByReportedPinId(targetPin.getId());
        entityManager.clear();

        // when: 반려 이후 다른 회원이 같은 핀을 재신고
        Member reporter3 = saveMember("신고자3");
        reportRepository.saveAndFlush(
                Report.createPinReport(reporter3, targetPin, ReportCategory.OTHER, "새로운 신고"));
        entityManager.clear();

        List<ReportReason> result = reportRepository.findReasonsByReportedPinIds(List.of(targetPin.getId()));

        // then: 반려된 과거 사유 2건은 제외되고 재신고 사유만 조회된다
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reporterNickname()).isEqualTo("신고자3");
        assertThat(result.get(0).category()).isEqualTo(ReportCategory.OTHER);
    }

    @Test
    void DB에서도_기타가_아닌_신고의_상세_내용을_허용하지_않는다() {
        // given
        Member reporter = saveMember("신고자");
        Member reportedMember = saveMember("신고대상");

        // when & then
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO report (reporter_member_id, reported_member_id, category, detail)
                VALUES (?, ?, 'COMMERCIAL_OR_PROMOTIONAL', '상세 내용')
                """, reporter.getId(), reportedMember.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
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
                "report-test-track-" + java.util.UUID.randomUUID(),
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
