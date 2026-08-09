package com.example.plimap.domain.inquiry.service.query;

import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InquiryQueryService {

    Page<Inquiry> getInquiries(InquiryCategory category, Pageable pageable);

    Inquiry getInquiry(Long inquiryId);
}
