package com.example.plimap.domain.member.service.command.impl;

import com.example.plimap.domain.member.dto.request.TermsReqDTO;
import com.example.plimap.domain.member.dto.response.TermsResponse;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberTermsAgreement;
import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.exception.TermsErrorCode;
import com.example.plimap.domain.member.exception.TermsException;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.repository.MemberTermsAgreementRepository;
import com.example.plimap.domain.member.service.command.TermsCommandService;
import com.example.plimap.domain.member.service.query.TermsQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TermsCommandServiceImpl implements TermsCommandService {

    private final MemberRepository memberRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;
    private final TermsQueryService termsQueryService;

    @Override
    public List<TermsResponse.Result> agreeToTerms(Long memberId, TermsReqDTO.Agree request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        List<MemberTermsAgreement> agreements = request.agreements().stream()
                .map(item -> upsertAgreement(member, item))
                .toList();

        validateRequiredTermsAgreed(member);

        return agreements.stream()
                .map(TermsResponse.Result::from)
                .toList();
    }

    private MemberTermsAgreement upsertAgreement(Member member, TermsReqDTO.Agree.Item item) {
        Terms terms = termsQueryService.getActiveTermsByType(item.type());

        return memberTermsAgreementRepository.findByMember_IdAndTerms_Id(member.getId(), terms.getId())
                .map(agreement -> {
                    agreement.updateAgreement(item.agreed());
                    return agreement;
                })
                .orElseGet(() -> memberTermsAgreementRepository.save(
                        MemberTermsAgreement.create(member, terms, item.agreed())));
    }

    private void validateRequiredTermsAgreed(Member member) {
        boolean allRequiredAgreed = termsQueryService.getActiveTerms().stream()
                .filter(Terms::isRequired)
                .map(Terms::getType)
                .distinct()
                .map(termsQueryService::getActiveTermsByType)
                .allMatch(terms -> memberTermsAgreementRepository
                        .findByMember_IdAndTerms_Id(member.getId(), terms.getId())
                        .map(MemberTermsAgreement::isAgreed)
                        .orElse(false));

        if (!allRequiredAgreed) {
            throw new TermsException(TermsErrorCode.AGREEMENT_REQUIRED);
        }
    }
}
