package com.example.plimap.domain.admin.service.query;

import com.example.plimap.domain.admin.dto.response.AdminResponse;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.pin.enums.PinReportFilter;

public interface AdminQueryService {

    AdminResponse.ReportedPinPage getReportedPins(PinReportFilter filter, int page, int pageSize);

    AdminResponse.MemberPage getMembers(String query, MemberStatus status, int page, int pageSize);

    AdminResponse.MemberDetail getMemberDetail(Long memberId);

    AdminResponse.InquiryPage getInquiries(InquiryCategory category, String cursor, Integer pageSize);

    AdminResponse.InquiryDetail getInquiryDetail(Long inquiryId);
}
