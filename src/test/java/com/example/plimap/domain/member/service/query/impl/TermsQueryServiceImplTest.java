package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.dto.response.TermsResDTO;
import com.example.plimap.domain.member.entity.MemberTermsAgreement;
import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import com.example.plimap.domain.member.exception.TermsErrorCode;
import com.example.plimap.domain.member.exception.TermsException;
import com.example.plimap.domain.member.repository.MemberTermsAgreementRepository;
import com.example.plimap.domain.member.repository.TermsRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TermsQueryServiceImplTest {

    private final TermsRepository termsRepository = mock(TermsRepository.class);
    private final MemberTermsAgreementRepository memberTermsAgreementRepository = mock(MemberTermsAgreementRepository.class);
    private final TermsQueryServiceImpl termsQueryService =
            new TermsQueryServiceImpl(termsRepository, memberTermsAgreementRepository);

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

    @Test
    void 회원이_동의한_약관은_동의여부와_동의일시가_함께_조회된다() {
        // given
        Instant agreedAt = Instant.parse("2026-07-14T06:49:44Z");
        Terms agreedTerms = mock(Terms.class);
        Terms notAgreedTerms = mock(Terms.class);
        when(agreedTerms.getId()).thenReturn(1L);
        when(agreedTerms.getType()).thenReturn(TermsType.SERVICE);
        when(notAgreedTerms.getId()).thenReturn(2L);
        when(notAgreedTerms.getType()).thenReturn(TermsType.MARKETING);
        when(termsRepository.findAllByActiveTrueOrderByTypeAsc()).thenReturn(List.of(agreedTerms, notAgreedTerms));

        MemberTermsAgreement agreement = mock(MemberTermsAgreement.class);
        when(agreement.getTerms()).thenReturn(agreedTerms);
        when(agreement.isAgreed()).thenReturn(true);
        when(agreement.getAgreedAt()).thenReturn(agreedAt);
        when(memberTermsAgreementRepository.findAllByMember_Id(1L)).thenReturn(List.of(agreement));

        // when
        List<TermsResDTO.Result> result = termsQueryService.findTermsAgreementStatus(1L);

        // then
        assertThat(result)
                .extracting(TermsResDTO.Result::type, TermsResDTO.Result::agreed, TermsResDTO.Result::agreedAt)
                .containsExactly(
                        tuple(TermsType.SERVICE, true, agreedAt),
                        tuple(TermsType.MARKETING, false, null)
                );
    }

    @Test
    void 동의_내역이_없는_회원은_모든_약관이_동의여부_false이고_동의일시가_없다() {
        // given
        Terms terms = mock(Terms.class);
        when(terms.getId()).thenReturn(1L);
        when(terms.getType()).thenReturn(TermsType.SERVICE);
        when(termsRepository.findAllByActiveTrueOrderByTypeAsc()).thenReturn(List.of(terms));
        when(memberTermsAgreementRepository.findAllByMember_Id(1L)).thenReturn(List.of());

        // when
        List<TermsResDTO.Result> result = termsQueryService.findTermsAgreementStatus(1L);

        // then
        assertThat(result)
                .extracting(TermsResDTO.Result::type, TermsResDTO.Result::agreed, TermsResDTO.Result::agreedAt)
                .containsExactly(tuple(TermsType.SERVICE, false, null));
    }
}
