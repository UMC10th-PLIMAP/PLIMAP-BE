package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {

    private final MemberRepository memberRepository;

    @Override
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull(nickname);
    }
}
