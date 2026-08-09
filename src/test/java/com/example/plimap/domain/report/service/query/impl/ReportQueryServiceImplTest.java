package com.example.plimap.domain.report.service.query.impl;

import com.example.plimap.domain.report.dto.ReportReason;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.repository.ReportRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
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
        ReportReason reasonForPin1 = new ReportReason(1L, ReportCategory.OTHER, "상세1", "신고자1", now);
        ReportReason firstReasonForPin2 = new ReportReason(2L, ReportCategory.ABUSE_OR_HATE_SPEECH, null, "신고자2", now);
        ReportReason secondReasonForPin2 = new ReportReason(2L, ReportCategory.OBSCENE_OR_HARMFUL, null, "신고자3", now);
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
}
