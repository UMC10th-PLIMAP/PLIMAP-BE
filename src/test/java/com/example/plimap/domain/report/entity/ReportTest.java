package com.example.plimap.domain.report.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.report.enums.ReportCategory;
import org.junit.jupiter.api.Test;

class ReportTest {

    @Test
    void 회원_신고를_생성한다() {
        // given
        Member reporter = member("신고자");
        Member reportedMember = member("신고대상");

        // when
        Report report = Report.createMemberReport(
                reporter,
                reportedMember,
                ReportCategory.PERSONAL_INFORMATION_EXPOSURE,
                null
        );

        // then
        assertThat(report.getReporter()).isSameAs(reporter);
        assertThat(report.getReportedMember()).isSameAs(reportedMember);
        assertThat(report.getReportedPin()).isNull();
        assertThat(report.getCategory()).isEqualTo(ReportCategory.PERSONAL_INFORMATION_EXPOSURE);
        assertThat(report.getDetail()).isNull();
    }

    @Test
    void PIN_신고를_생성한다() {
        // given
        Member reporter = member("신고자");
        Member pinAuthor = member("작성자");
        Pin reportedPin = pin(pinAuthor);

        // when
        Report report = Report.createPinReport(
                reporter,
                reportedPin,
                ReportCategory.OTHER,
                "가"
        );

        // then
        assertThat(report.getReporter()).isSameAs(reporter);
        assertThat(report.getReportedMember()).isNull();
        assertThat(report.getReportedPin()).isSameAs(reportedPin);
        assertThat(report.getCategory()).isEqualTo(ReportCategory.OTHER);
        assertThat(report.getDetail()).isEqualTo("가");
    }

    @Test
    void 기타_신고_내용은_최대_길이를_제한하지_않는다() {
        // given
        String detail = "가".repeat(10_000);

        // when
        Report report = Report.createMemberReport(
                member("신고자"),
                member("신고대상"),
                ReportCategory.OTHER,
                detail
        );

        // then
        assertThat(report.getDetail()).hasSize(10_000);
    }

    @Test
    void 신고_대상은_정확히_하나여야_한다() {
        // given
        Member reporter = member("신고자");

        // when & then
        assertThatThrownBy(() -> Report.createMemberReport(
                reporter,
                null,
                ReportCategory.OBSCENE_OR_HARMFUL,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exactly one report target is required");
    }

    @Test
    void 자기_자신은_회원_신고할_수_없다() {
        // given
        Member reporter = member("신고자");

        // when & then
        assertThatThrownBy(() -> Report.createMemberReport(
                reporter,
                reporter,
                ReportCategory.ABUSE_OR_HATE_SPEECH,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("member cannot report self");
    }

    @Test
    void 기타_신고는_공백이_아닌_내용이_필수다() {
        // given
        Member reporter = member("신고자");
        Member reportedMember = member("신고대상");

        // when & then
        assertThatThrownBy(() -> Report.createMemberReport(
                reporter,
                reportedMember,
                ReportCategory.OTHER,
                " "
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("detail is required for OTHER category");
    }

    @Test
    void 기타가_아닌_신고는_상세_내용을_허용하지_않는다() {
        // given
        Member reporter = member("신고자");
        Member reportedMember = member("신고대상");

        // when & then
        assertThatThrownBy(() -> Report.createMemberReport(
                reporter,
                reportedMember,
                ReportCategory.COMMERCIAL_OR_PROMOTIONAL,
                "상세 내용"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("detail is allowed only for OTHER category");
    }

    private Member member(String nickname) {
        return Member.builder()
                .nickname(nickname)
                .name(nickname)
                .build();
    }

    private Pin pin(Member author) {
        return Pin.builder()
                .member(author)
                .clipStartMs(0)
                .introduction("테스트 PIN")
                .isFeedPublic(true)
                .build();
    }
}
