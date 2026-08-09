package com.example.plimap.domain.inquiry.service.command.impl;

import com.example.plimap.domain.inquiry.dto.request.InquiryRequest;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.repository.InquiryRepository;
import com.example.plimap.domain.inquiry.service.command.InquiryCommandService;
import com.example.plimap.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryCommandServiceImpl implements InquiryCommandService {

    private final InquiryRepository inquiryRepository;

    @Override
    public void createInquiry(Member member, InquiryRequest.Create request) {
        Inquiry inquiry = Inquiry.create(
                member,
                request.category(),
                request.title(),
                request.content(),
                request.contactEmail()
        );
        inquiryRepository.save(inquiry);
    }
}
