package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.dto.response.TermsResponse;
import com.example.plimap.domain.member.entity.MemberTermsAgreement;
import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import com.example.plimap.domain.member.exception.TermsErrorCode;
import com.example.plimap.domain.member.exception.TermsException;
import com.example.plimap.domain.member.repository.MemberTermsAgreementRepository;
import com.example.plimap.domain.member.repository.TermsRepository;
import com.example.plimap.domain.member.service.query.TermsQueryService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsQueryServiceImpl implements TermsQueryService {

    private final TermsRepository termsRepository;
    private final MemberTermsAgreementRepository memberTermsAgreementRepository;

    @Override
    public List<Terms> getActiveTerms() {
        return termsRepository.findAllByActiveTrueOrderByTypeAsc();
    }

    @Override
    public Terms getActiveTermsByType(TermsType type) {
        return termsRepository.findFirstByTypeAndActiveTrueOrderByEffectiveAtDesc(type)
                .orElseThrow(() -> new TermsException(TermsErrorCode.TERMS_NOT_FOUND));
    }

    @Override
    public List<TermsResponse.Result> findTermsAgreementStatus(Long memberId) {
        Map<Long, MemberTermsAgreement> agreementsByTermsId = memberTermsAgreementRepository
                .findAllByMember_Id(memberId).stream()
                .collect(Collectors.toMap(agreement -> agreement.getTerms().getId(), Function.identity()));

        return getActiveTerms().stream()
                .map(terms -> {
                    MemberTermsAgreement agreement = agreementsByTermsId.get(terms.getId());
                    return agreement != null ? TermsResponse.Result.from(agreement) : TermsResponse.Result.notAgreed(terms);
                })
                .toList();
    }
}
