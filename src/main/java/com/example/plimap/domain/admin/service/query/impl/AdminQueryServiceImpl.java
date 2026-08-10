package com.example.plimap.domain.admin.service.query.impl;

import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.auth.service.query.AuthQueryService;
import com.example.plimap.domain.inquiry.dto.Pagination;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.inquiry.service.query.InquiryQueryService;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.pin.dto.ReportedPinInfo;
import com.example.plimap.domain.pin.enums.PinReportFilter;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.report.dto.ReportReason;
import com.example.plimap.domain.report.service.query.ReportQueryService;
import com.example.plimap.domain.admin.service.query.AdminQueryService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * pin/member/report/auth/inquiry 도메인 QueryService만 조합하는 최상위 조회 오케스트레이터.
 * AdminCommandServiceImpl과 동일한 이유(순환 빈 의존 회피)로 admin 도메인에 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQueryServiceImpl implements AdminQueryService {

    private static final int REPORT_HIDE_THRESHOLD = 10;

    private final PinQueryService pinQueryService;
    private final MemberQueryService memberQueryService;
    private final ReportQueryService reportQueryService;
    private final AuthQueryService authQueryService;
    private final InquiryQueryService inquiryQueryService;

    @Override
    public AdminResDTO.ReportedPinPage getReportedPins(PinReportFilter filter, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<ReportedPinInfo> pinPage = pinQueryService.findReportedPins(filter, pageable);

        List<Long> pinIds = pinPage.getContent().stream()
                .map(ReportedPinInfo::pinId)
                .toList();
        Map<Long, List<ReportReason>> reasonsByPin = reportQueryService.findReasonsByPinIds(pinIds);

        List<AdminResDTO.ReportedPinItem> items = pinPage.getContent().stream()
                .map(info -> toReportedPinItem(info, reasonsByPin.getOrDefault(info.pinId(), List.of())))
                .toList();

        return new AdminResDTO.ReportedPinPage(items, pinPage.getTotalElements(), page, pageSize);
    }

    @Override
    public AdminResDTO.MemberPage getMembers(String query, MemberStatus status, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Member> memberPage = memberQueryService.searchMembers(query, status, pageable);

        List<AdminResDTO.MemberSummary> items = memberPage.getContent().stream()
                .map(AdminResDTO.MemberSummary::from)
                .toList();

        return new AdminResDTO.MemberPage(items, memberPage.getTotalElements(), page, pageSize);
    }

    @Override
    public AdminResDTO.MemberDetail getMemberDetail(Long memberId) {
        Member member = memberQueryService.getMemberById(memberId);
        String email = authQueryService.findEmailByMemberId(memberId).orElse(null);
        return AdminResDTO.MemberDetail.from(member, email);
    }

    @Override
    public AdminResDTO.InquiryPage getInquiries(InquiryCategory category, String cursor, Integer pageSize) {
        Pagination<Inquiry> inquiryPage = inquiryQueryService.getInquiries(category, cursor, pageSize);

        List<AdminResDTO.InquirySummary> items = inquiryPage.data().stream()
                .map(AdminResDTO.InquirySummary::from)
                .toList();

        return new AdminResDTO.InquiryPage(items, inquiryPage.nextCursor(), inquiryPage.hasNext(), pageSize);
    }

    @Override
    public AdminResDTO.InquiryDetail getInquiryDetail(Long inquiryId) {
        return AdminResDTO.InquiryDetail.from(inquiryQueryService.getInquiry(inquiryId));
    }

    private AdminResDTO.ReportedPinItem toReportedPinItem(ReportedPinInfo info, List<ReportReason> reasons) {
        List<AdminResDTO.ReportReasonItem> reasonItems = reasons.stream()
                .map(reason -> new AdminResDTO.ReportReasonItem(
                        reason.category(), reason.detail(), reason.reporterNickname(), reason.createdAt()))
                .toList();

        return new AdminResDTO.ReportedPinItem(
                info.pinId(),
                info.pinTitle(),
                info.pinLocation(),
                info.authorMemberId(),
                info.authorNickname(),
                info.authorStatus(),
                info.reportCount(),
                info.reportCount() >= REPORT_HIDE_THRESHOLD,
                info.createdAt(),
                reasonItems
        );
    }
}
