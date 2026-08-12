package com.example.plimap.domain.report.service.query.impl;

import com.example.plimap.domain.report.dto.ReportReason;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.exception.ReportErrorCode;
import com.example.plimap.domain.report.exception.ReportException;
import com.example.plimap.domain.report.repository.ReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReportQueryServiceImplTest {

    private final ReportRepository reportRepository = mock(ReportRepository.class);
    private final ReportQueryServiceImpl reportQueryService = new ReportQueryServiceImpl(reportRepository);

    @Test
    void 핀id_목록으로_신고_사유를_핀별로_그룹핑한다() {
        Instant now = Instant.now();
        ReportReason reasonForPin1 = new ReportReason(10L, 1L, ReportCategory.OTHER, "상세1", "신고자1", now);
        ReportReason firstReasonForPin2 = new ReportReason(20L, 2L, ReportCategory.ABUSE_OR_HATE_SPEECH, null, "신고자2", now);
        ReportReason secondReasonForPin2 = new ReportReason(21L, 2L, ReportCategory.OBSCENE_OR_HARMFUL, null, "신고자3", now);
        when(reportRepository.findReasonsByReportedPinIds(List.of(1L, 2L)))
                .thenReturn(List.of(reasonForPin1, firstReasonForPin2, secondReasonForPin2));

        Map<Long, List<ReportReason>> result = reportQueryService.findReasonsByPinIds(List.of(1L, 2L));

        assertThat(result.get(1L)).containsExactly(reasonForPin1);
        assertThat(result.get(2L)).containsExactly(firstReasonForPin2, secondReasonForPin2);
    }

    @Test
    void 핀id_목록이_비어있으면_조회하지_않고_빈_Map을_반환한다() {
        Map<Long, List<ReportReason>> result = reportQueryService.findReasonsByPinIds(List.of());

        assertThat(result).isEmpty();
        verify(reportRepository, never()).findReasonsByReportedPinIds(anyList());
    }

    @Test
    void 신고id로_사유를_조회한다() {
        Instant now = Instant.now();
        ReportReason reason = new ReportReason(10L, 1L, ReportCategory.OTHER, "상세", "신고자", now);
        when(reportRepository.findReasonById(10L)).thenReturn(Optional.of(reason));

        ReportReason result = reportQueryService.getReasonById(10L);

        assertThat(result).isEqualTo(reason);
    }

    @Test
    void 존재하지_않는_신고id면_예외가_발생한다() {
        when(reportRepository.findReasonById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportQueryService.getReasonById(999L))
                .isInstanceOfSatisfying(ReportException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ReportErrorCode.REPORT_NOT_FOUND));
    }
}
