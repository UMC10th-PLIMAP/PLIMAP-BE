package com.example.plimap.domain.member.service.command.impl;

import com.example.plimap.domain.member.dto.request.TermsReqDTO;
import com.example.plimap.domain.member.dto.response.TermsResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberTermsAgreement;
import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.exception.TermsErrorCode;
import com.example.plimap.domain.member.exception.TermsException;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.repository.MemberTermsAgreementRepository;
import com.example.plimap.domain.member.service.query.TermsQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TermsCommandServiceImplTest {

    private static final Long MEMBER_ID = 1L;

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberTermsAgreementRepository memberTermsAgreementRepository =
            mock(MemberTermsAgreementRepository.class);
    private final TermsQueryService termsQueryService = mock(TermsQueryService.class);

    private final TermsCommandServiceImpl termsCommandService = new TermsCommandServiceImpl(
            memberRepository, memberTermsAgreementRepository, termsQueryService);

    @Test
    void 존재하지_않는_회원이면_예외가_발생한다() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> termsCommandService.agreeToTerms(MEMBER_ID, agree(TermsType.SERVICE, true)))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 기존_동의_내역이_있으면_갱신하고_필수_약관에_모두_동의했으면_성공한다() {
        Member member = member(MEMBER_ID);
        Terms serviceTerms = terms(1L, true, TermsType.SERVICE);
        MemberTermsAgreement existingAgreement = agreement(serviceTerms, true);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(termsQueryService.getActiveTermsByType(TermsType.SERVICE)).thenReturn(serviceTerms);
        when(memberTermsAgreementRepository.findByMember_IdAndTerms_Id(MEMBER_ID, 1L))
                .thenReturn(Optional.of(existingAgreement));
        when(termsQueryService.getActiveTerms()).thenReturn(List.of(serviceTerms));

        List<TermsResDTO.Result> result = termsCommandService.agreeToTerms(MEMBER_ID, agree(TermsType.SERVICE, true));

        assertThat(result).extracting(TermsResDTO.Result::type, TermsResDTO.Result::agreed)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(TermsType.SERVICE, true));
        verify(existingAgreement).updateAgreement(true);
        verify(memberTermsAgreementRepository, never()).save(any());
    }

    @Test
    void 필수가_아닌_약관은_기존_동의가_없으면_새로_생성한다() {
        Member member = member(MEMBER_ID);
        Terms marketingTerms = terms(2L, false, TermsType.MARKETING);
        MemberTermsAgreement savedAgreement = agreement(marketingTerms, false);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(termsQueryService.getActiveTermsByType(TermsType.MARKETING)).thenReturn(marketingTerms);
        when(memberTermsAgreementRepository.findByMember_IdAndTerms_Id(MEMBER_ID, 2L))
                .thenReturn(Optional.empty());
        when(memberTermsAgreementRepository.save(any(MemberTermsAgreement.class))).thenReturn(savedAgreement);
        when(termsQueryService.getActiveTerms()).thenReturn(List.of(marketingTerms));

        List<TermsResDTO.Result> result =
                termsCommandService.agreeToTerms(MEMBER_ID, agree(TermsType.MARKETING, false));

        assertThat(result).extracting(TermsResDTO.Result::type, TermsResDTO.Result::agreed)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(TermsType.MARKETING, false));

        ArgumentCaptor<MemberTermsAgreement> captor = ArgumentCaptor.forClass(MemberTermsAgreement.class);
        verify(memberTermsAgreementRepository).save(captor.capture());
        assertThat(captor.getValue().isAgreed()).isFalse();
        assertThat(captor.getValue().getMember()).isSameAs(member);
        assertThat(captor.getValue().getTerms()).isSameAs(marketingTerms);
    }

    @Test
    void 같은_유형의_구버전_활성_약관이_남아있어도_최신_버전_동의로_검증을_통과한다() {
        Member member = member(MEMBER_ID);
        Terms staleServiceTerms = terms(1L, true, TermsType.SERVICE);
        Terms latestServiceTerms = terms(3L, true, TermsType.SERVICE);
        MemberTermsAgreement existingAgreement = agreement(latestServiceTerms, true);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(termsQueryService.getActiveTermsByType(TermsType.SERVICE)).thenReturn(latestServiceTerms);
        when(memberTermsAgreementRepository.findByMember_IdAndTerms_Id(MEMBER_ID, 3L))
                .thenReturn(Optional.of(existingAgreement));
        // 구버전(id=1)이 비활성화되지 않은 채 활성 목록에 남아있는 상황을 재현
        when(termsQueryService.getActiveTerms()).thenReturn(List.of(staleServiceTerms, latestServiceTerms));

        List<TermsResDTO.Result> result = termsCommandService.agreeToTerms(MEMBER_ID, agree(TermsType.SERVICE, true));

        assertThat(result).extracting(TermsResDTO.Result::type, TermsResDTO.Result::agreed)
                .containsExactly(org.assertj.core.groups.Tuple.tuple(TermsType.SERVICE, true));
        verify(memberTermsAgreementRepository, never()).findByMember_IdAndTerms_Id(MEMBER_ID, 1L);
    }

    @Test
    void 필수_약관에_동의하지_않으면_예외가_발생한다() {
        Member member = member(MEMBER_ID);
        Terms serviceTerms = terms(1L, true, TermsType.SERVICE);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(termsQueryService.getActiveTermsByType(TermsType.SERVICE)).thenReturn(serviceTerms);
        when(memberTermsAgreementRepository.findByMember_IdAndTerms_Id(MEMBER_ID, 1L))
                .thenReturn(Optional.empty());
        MemberTermsAgreement savedAgreement = agreement(serviceTerms, false);
        when(memberTermsAgreementRepository.save(any(MemberTermsAgreement.class))).thenReturn(savedAgreement);
        when(termsQueryService.getActiveTerms()).thenReturn(List.of(serviceTerms));

        assertThatThrownBy(() -> termsCommandService.agreeToTerms(MEMBER_ID, agree(TermsType.SERVICE, false)))
                .isInstanceOfSatisfying(TermsException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(TermsErrorCode.AGREEMENT_REQUIRED));
    }

    private Member member(Long id) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(id);
        return member;
    }

    private Terms terms(Long id, boolean required, TermsType type) {
        Terms terms = mock(Terms.class);
        when(terms.getId()).thenReturn(id);
        when(terms.isRequired()).thenReturn(required);
        when(terms.getType()).thenReturn(type);
        return terms;
    }

    private MemberTermsAgreement agreement(Terms terms, boolean agreed) {
        MemberTermsAgreement agreement = mock(MemberTermsAgreement.class);
        when(agreement.getTerms()).thenReturn(terms);
        when(agreement.isAgreed()).thenReturn(agreed);
        when(agreement.getAgreedAt()).thenReturn(agreed ? Instant.now() : null);
        return agreement;
    }

    private TermsReqDTO.Agree agree(TermsType type, boolean agreed) {
        return new TermsReqDTO.Agree(List.of(new TermsReqDTO.Agree.Item(type, agreed)));
    }
}
