package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import com.example.plimap.domain.member.exception.TermsErrorCode;
import com.example.plimap.domain.member.exception.TermsException;
import com.example.plimap.domain.member.repository.TermsRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TermsQueryServiceImplTest {

    private final TermsRepository termsRepository = mock(TermsRepository.class);
    private final TermsQueryServiceImpl termsQueryService = new TermsQueryServiceImpl(termsRepository);

    @Test
    void 활성_약관_목록을_조회한다() {
        Terms terms = mock(Terms.class);
        when(termsRepository.findAllByActiveTrueOrderByTypeAsc()).thenReturn(List.of(terms));

        List<Terms> result = termsQueryService.getActiveTerms();

        assertThat(result).containsExactly(terms);
    }

    @Test
    void 활성_약관이_있으면_유형으로_조회한다() {
        Terms terms = mock(Terms.class);
        when(termsRepository.findFirstByTypeAndActiveTrueOrderByEffectiveAtDesc(TermsType.SERVICE))
                .thenReturn(Optional.of(terms));

        Terms result = termsQueryService.getActiveTermsByType(TermsType.SERVICE);

        assertThat(result).isSameAs(terms);
    }

    @Test
    void 활성_약관이_없으면_예외가_발생한다() {
        when(termsRepository.findFirstByTypeAndActiveTrueOrderByEffectiveAtDesc(TermsType.SERVICE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> termsQueryService.getActiveTermsByType(TermsType.SERVICE))
                .isInstanceOfSatisfying(TermsException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(TermsErrorCode.TERMS_NOT_FOUND));
    }
}
