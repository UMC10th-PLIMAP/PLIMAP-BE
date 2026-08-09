package com.example.plimap.domain.admin.service.query;

import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.pin.enums.PinReportFilter;

public interface AdminQueryService {

    AdminResDTO.ReportedPinPage getReportedPins(PinReportFilter filter, int page, int pageSize);

    AdminResDTO.MemberPage getMembers(String query, MemberStatus status, int page, int pageSize);

    AdminResDTO.MemberDetail getMemberDetail(Long memberId);

    AdminResDTO.InquiryPage getInquiries(InquiryCategory category, int page, int pageSize);

    AdminResDTO.InquiryDetail getInquiryDetail(Long inquiryId);
}
