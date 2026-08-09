package com.example.plimap.domain.inquiry.service.command.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.example.plimap.domain.inquiry.dto.request.InquiryRequest;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.inquiry.repository.InquiryRepository;
import com.example.plimap.domain.member.entity.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InquiryCommandServiceImplTest {

    @InjectMocks
    private InquiryCommandServiceImpl inquiryCommandService;

    @Mock
    private InquiryRepository inquiryRepository;

    @Test
    void 로그인_사용자의_문의를_등록한다() {
        // given
        Member member = Member.builder().nickname("작성자").build();
        InquiryRequest.Create request = new InquiryRequest.Create(
                InquiryCategory.APP_BUG_OR_ERROR,
                "핀 재생이 안 돼요",
                "특정 곡의 PIN이 재생되지 않습니다.",
                "user@example.com"
        );

        // when
        inquiryCommandService.createInquiry(member, request);

        // then
        ArgumentCaptor<Inquiry> inquiryCaptor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(inquiryCaptor.capture());
        Inquiry savedInquiry = inquiryCaptor.getValue();
        assertThat(savedInquiry.getMember()).isSameAs(member);
        assertThat(savedInquiry.getCategory()).isEqualTo(InquiryCategory.APP_BUG_OR_ERROR);
        assertThat(savedInquiry.getTitle()).isEqualTo("핀 재생이 안 돼요");
        assertThat(savedInquiry.getContactEmail()).isEqualTo("user@example.com");
    }

    @Test
    void 비로그인_사용자의_문의는_작성자_없이_등록한다() {
        // given
        InquiryRequest.Create request = new InquiryRequest.Create(
                InquiryCategory.OTHER,
                "기타 문의",
                "문의 내용입니다.",
                "guest@example.com"
        );

        // when
        inquiryCommandService.createInquiry(null, request);

        // then
        ArgumentCaptor<Inquiry> inquiryCaptor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(inquiryCaptor.capture());
        assertThat(inquiryCaptor.getValue().getMember()).isNull();
    }
}
