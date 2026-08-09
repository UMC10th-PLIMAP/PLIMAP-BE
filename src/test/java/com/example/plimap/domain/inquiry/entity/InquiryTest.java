package com.example.plimap.domain.inquiry.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.member.entity.Member;
import org.junit.jupiter.api.Test;

class InquiryTest {

    @Test
    void 로그인_사용자의_문의를_생성한다() {
        // given
        Member member = Member.builder().nickname("작성자").build();

        // when
        Inquiry inquiry = Inquiry.create(
                member,
                InquiryCategory.APP_BUG_OR_ERROR,
                "핀 재생이 안 돼요",
                "특정 곡의 PIN이 재생되지 않습니다.",
                "user@example.com"
        );

        // then
        assertThat(inquiry.getMember()).isSameAs(member);
        assertThat(inquiry.getCategory()).isEqualTo(InquiryCategory.APP_BUG_OR_ERROR);
        assertThat(inquiry.getTitle()).isEqualTo("핀 재생이 안 돼요");
        assertThat(inquiry.getContent()).isEqualTo("특정 곡의 PIN이 재생되지 않습니다.");
        assertThat(inquiry.getContactEmail()).isEqualTo("user@example.com");
    }

    @Test
    void 비로그인_사용자의_문의는_작성자가_연결되지_않는다() {
        // when
        Inquiry inquiry = Inquiry.create(
                null,
                InquiryCategory.OTHER,
                "기타 문의",
                "문의 내용입니다.",
                "guest@example.com"
        );

        // then
        assertThat(inquiry.getMember()).isNull();
    }
}
