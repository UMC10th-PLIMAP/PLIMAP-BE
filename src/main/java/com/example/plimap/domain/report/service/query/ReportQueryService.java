package com.example.plimap.domain.report.service.query;

import com.example.plimap.domain.report.dto.ReportReason;
import java.util.List;
import java.util.Map;

public interface ReportQueryService {

    Map<Long, List<ReportReason>> findReasonsByPinIds(List<Long> pinIds);

    ReportReason getReasonById(Long reportId);
}
