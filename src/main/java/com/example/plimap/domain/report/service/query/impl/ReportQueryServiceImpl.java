package com.example.plimap.domain.report.service.query.impl;

import com.example.plimap.domain.report.dto.ReportReason;
import com.example.plimap.domain.report.repository.ReportRepository;
import com.example.plimap.domain.report.service.query.ReportQueryService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportQueryServiceImpl implements ReportQueryService {

    private final ReportRepository reportRepository;

    @Override
    public Map<Long, List<ReportReason>> findReasonsByPinIds(List<Long> pinIds) {
        if (pinIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return reportRepository.findReasonsByReportedPinIds(pinIds).stream()
                .collect(Collectors.groupingBy(ReportReason::pinId));
    }
}
