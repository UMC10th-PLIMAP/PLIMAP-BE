package com.example.plimap.domain.report.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.report.dto.request.ReportRequest;
import com.example.plimap.domain.report.entity.Report;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.exception.ReportErrorCode;
import com.example.plimap.domain.report.exception.ReportException;
import com.example.plimap.domain.report.repository.ReportRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportCommandServiceImplTest {

    private static final Long REPORTER_ID = 1L;
    private static final Long TARGET_ID = 2L;

    @InjectMocks
    private ReportCommandServiceImpl reportCommandService;

    @Mock
    private MemberQueryService memberQueryService;

    @Mock
    private MemberCommandService memberCommandService;

    @Mock
    private PinQueryService pinQueryService;

    @Mock
    private PinCommandService pinCommandService;

    @Mock
    private ReportRepository reportRepository;

    private Member reporter;

    @BeforeEach
    void setUp() {
        reporter = mock(Member.class);
        lenient().when(reporter.getId()).thenReturn(REPORTER_ID);
    }

    @Test
    void 회원_신고에_성공한다() {
        // given
        Member reportedMember = member(TARGET_ID);
        when(memberQueryService.getActiveMember(TARGET_ID)).thenReturn(reportedMember);

        // when
        reportCommandService.reportMember(reporter, TARGET_ID, request(ReportCategory.OBSCENE_OR_HARMFUL, null));

        // then
        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(reportCaptor.capture());
        Report savedReport = reportCaptor.getValue();
        assertThat(savedReport.getReporter()).isSameAs(reporter);
        assertThat(savedReport.getReportedMember()).isSameAs(reportedMember);
        assertThat(savedReport.getReportedPin()).isNull();
        verify(memberCommandService).increaseReportCount(TARGET_ID);
    }

    @Test
    void PIN_신고에_성공한다() {
        // given
        Member pinAuthor = member(TARGET_ID);
        Pin reportedPin = pin(pinAuthor, true);
        when(pinQueryService.getActivePin(TARGET_ID)).thenReturn(reportedPin);

        // when
        reportCommandService.reportPin(reporter, TARGET_ID, request(ReportCategory.OTHER, "가"));

        // then
        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(reportCaptor.capture());
        Report savedReport = reportCaptor.getValue();
        assertThat(savedReport.getReporter()).isSameAs(reporter);
        assertThat(savedReport.getReportedMember()).isNull();
        assertThat(savedReport.getReportedPin()).isSameAs(reportedPin);
        assertThat(savedReport.getDetail()).isEqualTo("가");
        verify(pinCommandService).increaseReportCount(TARGET_ID);
    }

    @Test
    void 동일_회원을_중복_신고하면_예외가_발생한다() {
        // given
        Member reportedMember = member(TARGET_ID);
        when(memberQueryService.getActiveMember(TARGET_ID)).thenReturn(reportedMember);
        when(reportRepository.existsByReporter_IdAndReportedMember_Id(REPORTER_ID, TARGET_ID))
                .thenReturn(true);

        // when & then
        assertReportError(
                () -> reportCommandService.reportMember(
                        reporter,
                        TARGET_ID,
                        request(ReportCategory.ABUSE_OR_HATE_SPEECH, null)
                ),
                ReportErrorCode.REPORT_MEMBER_ALREADY_EXISTS
        );
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동일_PIN을_중복_신고하면_예외가_발생한다() {
        // given
        Member pinAuthor = member(TARGET_ID);
        Pin reportedPin = pin(pinAuthor, true);
        when(pinQueryService.getActivePin(TARGET_ID)).thenReturn(reportedPin);
        when(reportRepository.existsByReporter_IdAndReportedPin_Id(REPORTER_ID, TARGET_ID))
                .thenReturn(true);

        // when & then
        assertReportError(
                () -> reportCommandService.reportPin(
                        reporter,
                        TARGET_ID,
                        request(ReportCategory.COMMERCIAL_OR_PROMOTIONAL, null)
                ),
                ReportErrorCode.REPORT_PIN_ALREADY_EXISTS
        );
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 자기_자신을_신고하면_예외가_발생한다() {
        // given
        when(memberQueryService.getActiveMember(REPORTER_ID)).thenReturn(reporter);

        // when & then
        assertReportError(
                () -> reportCommandService.reportMember(
                        reporter,
                        REPORTER_ID,
                        request(ReportCategory.PERSONAL_INFORMATION_EXPOSURE, null)
                ),
                ReportErrorCode.REPORT_SELF_NOT_ALLOWED
        );
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 자신이_작성한_PIN을_신고하면_예외가_발생한다() {
        // given
        Pin reportedPin = pin(reporter, true);
        when(pinQueryService.getActivePin(TARGET_ID)).thenReturn(reportedPin);

        // when & then
        assertReportError(
                () -> reportCommandService.reportPin(
                        reporter,
                        TARGET_ID,
                        request(ReportCategory.OBSCENE_OR_HARMFUL, null)
                ),
                ReportErrorCode.REPORT_OWN_PIN_NOT_ALLOWED
        );
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 공개_피드가_아닌_PIN을_신고하면_예외가_발생한다() {
        // given
        Member pinAuthor = member(TARGET_ID);
        Pin reportedPin = pin(pinAuthor, false);
        when(pinQueryService.getActivePin(TARGET_ID)).thenReturn(reportedPin);

        // when & then
        assertReportError(
                () -> reportCommandService.reportPin(
                        reporter,
                        TARGET_ID,
                        request(ReportCategory.OBSCENE_OR_HARMFUL, null)
                ),
                ReportErrorCode.REPORT_PRIVATE_PIN_NOT_ALLOWED
        );
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 존재하지_않거나_삭제된_회원은_신고할_수_없다() {
        // given
        MemberException exception = new MemberException(MemberErrorCode.MEMBER_NOT_FOUND);
        when(memberQueryService.getActiveMember(TARGET_ID)).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> reportCommandService.reportMember(
                reporter,
                TARGET_ID,
                request(ReportCategory.OBSCENE_OR_HARMFUL, null)
        )).isSameAs(exception);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 존재하지_않거나_삭제된_PIN은_신고할_수_없다() {
        // given
        PinException exception = new PinException(PinErrorCode.PIN_NOT_FOUND);
        when(pinQueryService.getActivePin(TARGET_ID)).thenThrow(exception);

        // when & then
        assertThatThrownBy(() -> reportCommandService.reportPin(
                reporter,
                TARGET_ID,
                request(ReportCategory.OBSCENE_OR_HARMFUL, null)
        )).isSameAs(exception);
        verify(reportRepository, never()).saveAndFlush(any());
    }

    @Test
    void 회원_신고_저장_중_유니크_위반은_중복_신고_예외로_변환한다() {
        // given
        Member reportedMember = member(TARGET_ID);
        when(memberQueryService.getActiveMember(TARGET_ID)).thenReturn(reportedMember);
        doThrow(new DataIntegrityViolationException("duplicate member report"))
                .when(reportRepository).saveAndFlush(any(Report.class));

        // when & then
        assertReportError(
                () -> reportCommandService.reportMember(
                        reporter,
                        TARGET_ID,
                        request(ReportCategory.OBSCENE_OR_HARMFUL, null)
                ),
                ReportErrorCode.REPORT_MEMBER_ALREADY_EXISTS
        );
    }

    @Test
    void PIN_신고_저장_중_유니크_위반은_중복_신고_예외로_변환한다() {
        // given
        Member pinAuthor = member(TARGET_ID);
        Pin reportedPin = pin(pinAuthor, true);
        when(pinQueryService.getActivePin(TARGET_ID)).thenReturn(reportedPin);
        doThrow(new DataIntegrityViolationException("duplicate pin report"))
                .when(reportRepository).saveAndFlush(any(Report.class));

        // when & then
        assertReportError(
                () -> reportCommandService.reportPin(
                        reporter,
                        TARGET_ID,
                        request(ReportCategory.OBSCENE_OR_HARMFUL, null)
                ),
                ReportErrorCode.REPORT_PIN_ALREADY_EXISTS
        );
    }

    @Test
    void 회원_기준으로_신고당한_이력을_삭제한다() {
        // given
        Long targetMemberId = TARGET_ID;

        // when
        reportCommandService.deleteReportsAgainstMember(targetMemberId);

        // then
        verify(reportRepository).deleteAllByReportedMemberId(targetMemberId);
    }

    @Test
    void 회원이_신고한_이력을_삭제하면_대상의_신고누적을_먼저_보정한다() {
        // given
        Long reporterId = REPORTER_ID;
        Long reportedPinId = 10L;
        Long reportedMemberId = 20L;
        when(reportRepository.findReportedPinIdsByReporterId(reporterId)).thenReturn(List.of(reportedPinId));
        when(reportRepository.findReportedMemberIdsByReporterId(reporterId)).thenReturn(List.of(reportedMemberId));

        // when
        reportCommandService.deleteReportsByReporter(reporterId);

        // then
        InOrder order = inOrder(pinCommandService, memberCommandService, reportRepository);
        order.verify(pinCommandService).decreaseReportCount(reportedPinId);
        order.verify(memberCommandService).decreaseReportCount(reportedMemberId);
        order.verify(reportRepository).deleteAllByReporterId(reporterId);
    }

    @Test
    void 핀_신고를_리뷰_완료로_표시한다() {
        // given
        Long pinId = 10L;

        // when
        reportCommandService.markPinReportsReviewed(pinId);

        // then
        verify(reportRepository).markReviewedByReportedPinId(pinId);
    }

    private Member member(Long id) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(id);
        return member;
    }

    private Pin pin(Member author, boolean feedPublic) {
        Pin pin = mock(Pin.class);
        when(pin.getMember()).thenReturn(author);
        lenient().when(pin.isFeedPublic()).thenReturn(feedPublic);
        return pin;
    }

    private ReportRequest.Create request(ReportCategory category, String detail) {
        return new ReportRequest.Create(category, detail);
    }

    private void assertReportError(Runnable action, ReportErrorCode expectedErrorCode) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ReportException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(expectedErrorCode));
    }
}