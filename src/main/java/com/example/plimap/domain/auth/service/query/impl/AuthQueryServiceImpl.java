package com.example.plimap.domain.auth.service.query.impl;

import com.example.plimap.domain.auth.entity.SocialAccount;
import com.example.plimap.domain.auth.repository.SocialAccountRepository;
import com.example.plimap.domain.auth.service.query.AuthQueryService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryServiceImpl implements AuthQueryService {

    private final SocialAccountRepository socialAccountRepository;

    @Override
    public Optional<String> findEmailByMemberId(Long memberId) {
        return Optional.ofNullable(findEmailsByMemberIds(List.of(memberId)).get(memberId));
    }

    @Override
    public Map<Long, String> findEmailsByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return socialAccountRepository.findByMember_IdInOrderByCreatedAtAsc(memberIds).stream()
                .filter(socialAccount -> socialAccount.getEmail() != null)
                .collect(Collectors.toMap(
                        socialAccount -> socialAccount.getMember().getId(),
                        SocialAccount::getEmail,
                        (existing, replacement) -> existing
                ));
    }
}
