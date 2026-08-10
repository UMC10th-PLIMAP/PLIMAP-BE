package com.example.plimap.domain.inquiry.service.query;

import com.example.plimap.domain.inquiry.dto.Pagination;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;

public interface InquiryQueryService {

    Pagination<Inquiry> getInquiries(InquiryCategory category, String cursor, Integer pageSize);

    Inquiry getInquiry(Long inquiryId);
}
