package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.entity.Terms;
import com.example.plimap.domain.member.enums.TermsType;
import com.example.plimap.domain.member.exception.TermsErrorCode;
import com.example.plimap.domain.member.exception.TermsException;
import com.example.plimap.domain.member.repository.TermsRepository;
import com.example.plimap.domain.member.service.query.TermsQueryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsQueryServiceImpl implements TermsQueryService {

    private final TermsRepository termsRepository;

    @Override
    public List<Terms> getActiveTerms() {
        return termsRepository.findAllByActiveTrueOrderByTypeAsc();
    }

    @Override
    public Terms getActiveTermsByType(TermsType type) {
        return termsRepository.findFirstByTypeAndActiveTrueOrderByEffectiveAtDesc(type)
                .orElseThrow(() -> new TermsException(TermsErrorCode.TERMS_NOT_FOUND));
    }
}
