package com.example.plimap.domain.inquiry.service.command;

import com.example.plimap.domain.inquiry.dto.request.InquiryRequest;
import com.example.plimap.domain.member.entity.Member;

public interface InquiryCommandService {

    void createInquiry(Member member, InquiryRequest.Create request);
}
