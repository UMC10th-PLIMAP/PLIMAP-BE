package com.example.plimap.domain.inquiry.service.query.impl;

import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.inquiry.exception.InquiryErrorCode;
import com.example.plimap.domain.inquiry.exception.InquiryException;
import com.example.plimap.domain.inquiry.repository.InquiryRepository;
import com.example.plimap.domain.inquiry.service.query.InquiryQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryQueryServiceImpl implements InquiryQueryService {

    private final InquiryRepository inquiryRepository;

    @Override
    public Page<Inquiry> getInquiries(InquiryCategory category, Pageable pageable) {
        if (category == null) {
            return inquiryRepository.findAllByOrderByCreatedAtDescIdDesc(pageable);
        }
        return inquiryRepository.findByCategoryOrderByCreatedAtDescIdDesc(category, pageable);
    }

    @Override
    public Inquiry getInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new InquiryException(InquiryErrorCode.INQUIRY_NOT_FOUND));
    }
}
