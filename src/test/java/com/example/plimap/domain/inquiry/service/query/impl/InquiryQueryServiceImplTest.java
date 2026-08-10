package com.example.plimap.domain.inquiry.service.query.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.inquiry.exception.InquiryErrorCode;
import com.example.plimap.domain.inquiry.exception.InquiryException;
import com.example.plimap.domain.inquiry.repository.InquiryRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class InquiryQueryServiceImplTest {

    @InjectMocks
    private InquiryQueryServiceImpl inquiryQueryService;

    @Mock
    private InquiryRepository inquiryRepository;

    @Test
    void 카테고리가_없으면_전체_문의를_최신순으로_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Inquiry inquiry = Inquiry.create(null, InquiryCategory.OTHER, "제목", "내용", "guest@example.com");
        Page<Inquiry> page = new PageImpl<>(java.util.List.of(inquiry), pageable, 1);
        when(inquiryRepository.findAllByOrderByCreatedAtDescIdDesc(pageable)).thenReturn(page);

        // when
        Page<Inquiry> result = inquiryQueryService.getInquiries(null, pageable);

        // then
        assertThat(result.getContent()).containsExactly(inquiry);
        verify(inquiryRepository, never()).findByCategoryOrderByCreatedAtDescIdDesc(any(), any());
    }

    @Test
    void 카테고리가_있으면_해당_카테고리만_최신순으로_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Inquiry inquiry = Inquiry.create(null, InquiryCategory.APP_BUG_OR_ERROR, "제목", "내용", "guest@example.com");
        Page<Inquiry> page = new PageImpl<>(java.util.List.of(inquiry), pageable, 1);
        when(inquiryRepository.findByCategoryOrderByCreatedAtDescIdDesc(InquiryCategory.APP_BUG_OR_ERROR, pageable))
                .thenReturn(page);

        // when
        Page<Inquiry> result = inquiryQueryService.getInquiries(InquiryCategory.APP_BUG_OR_ERROR, pageable);

        // then
        assertThat(result.getContent()).containsExactly(inquiry);
        verify(inquiryRepository, never()).findAllByOrderByCreatedAtDescIdDesc(any());
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
