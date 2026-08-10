package com.example.plimap.domain.inquiry.repository.query;

import com.example.plimap.domain.inquiry.dto.Pagination;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;

public interface InquiryQueryRepository {

    Pagination<Inquiry> findInquiries(InquiryCategory category, String cursor, Integer pageSize);
}
