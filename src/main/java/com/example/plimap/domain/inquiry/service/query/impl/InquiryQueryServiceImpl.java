package com.example.plimap.domain.inquiry.service.query.impl;

import com.example.plimap.domain.inquiry.dto.Pagination;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.inquiry.exception.InquiryErrorCode;
import com.example.plimap.domain.inquiry.exception.InquiryException;
import com.example.plimap.domain.inquiry.repository.InquiryRepository;
import com.example.plimap.domain.inquiry.repository.query.InquiryQueryRepository;
import com.example.plimap.domain.inquiry.service.query.InquiryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryQueryServiceImpl implements InquiryQueryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryQueryRepository inquiryQueryRepository;

    @Override
    public Pagination<Inquiry> getInquiries(InquiryCategory category, String cursor, Integer pageSize) {
        return inquiryQueryRepository.findInquiries(category, cursor, pageSize);
    }

    @Override
    public Inquiry getInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));
    }
}
