package com.example.plimap.domain.inquiry.service.query.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.inquiry.dto.Pagination;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.inquiry.exception.InquiryErrorCode;
import com.example.plimap.domain.inquiry.exception.InquiryException;
import com.example.plimap.domain.inquiry.repository.InquiryRepository;
import com.example.plimap.domain.inquiry.repository.query.InquiryQueryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InquiryQueryServiceImplTest {

    @InjectMocks
    private InquiryQueryServiceImpl inquiryQueryService;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private InquiryQueryRepository inquiryQueryRepository;

    @Test
    void 문의_목록_조회는_커서_기반_조회를_그대로_위임한다() {
        // given
        Inquiry inquiry = Inquiry.create(null, InquiryCategory.APP_BUG_OR_ERROR, "제목", "내용", "guest@example.com");
        Pagination<Inquiry> page = Pagination.<Inquiry>builder()
                .data(List.of(inquiry))
                .nextCursor(null)
                .hasNext(false)
                .pageSize(10)
                .build();
        when(inquiryQueryRepository.findInquiries(InquiryCategory.APP_BUG_OR_ERROR, "cursor", 10))
                .thenReturn(page);

        // when
        Pagination<Inquiry> result = inquiryQueryService.getInquiries(InquiryCategory.APP_BUG_OR_ERROR, "cursor", 10);

        // then
        assertThat(result.data()).containsExactly(inquiry);
        verify(inquiryQueryRepository).findInquiries(InquiryCategory.APP_BUG_OR_ERROR, "cursor", 10);
    }

    @Test
    void 존재하는_문의를_단건_조회한다() {
        // given
        Inquiry inquiry = Inquiry.create(null, InquiryCategory.OTHER, "제목", "내용", "guest@example.com");
        when(inquiryRepository.findById(1L)).thenReturn(Optional.of(inquiry));

        // when
        Inquiry result = inquiryQueryService.getInquiry(1L);

        // then
        assertThat(result).isSameAs(inquiry);
    }

    @Test
    void 존재하지_않는_문의를_조회하면_예외가_발생한다() {
        // given
        when(inquiryRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryQueryService.getInquiry(1L))
                .isInstanceOfSatisfying(InquiryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(InquiryErrorCode.INQUIRY_NOT_FOUND));
    }
}
