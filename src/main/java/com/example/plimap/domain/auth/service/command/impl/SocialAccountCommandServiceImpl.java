package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.repository.SocialAccountRepository;
import com.example.plimap.domain.auth.service.command.SocialAccountCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialAccountCommandServiceImpl implements SocialAccountCommandService {

    private final SocialAccountRepository socialAccountRepository;

    @Override
    @Transactional
    public void deleteByMemberId(Long memberId) {
        socialAccountRepository.deleteByMemberId(memberId);
    }
}
