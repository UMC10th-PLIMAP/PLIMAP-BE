package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.config.TestTokenProperties;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.auth.exception.AuthErrorCode;
import com.example.plimap.domain.auth.exception.AuthException;
import com.example.plimap.domain.auth.service.command.TestTokenCommandService;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.global.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Profile({"local", "dev"})
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestTokenCommandServiceImpl implements TestTokenCommandService {

    private final TestTokenProperties properties;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    @Override
    public String issueTestToken(Long memberId, String issueKey) {
        if (!properties.matches(issueKey)) {
            log.warn("Dev 테스트 토큰 발급이 거부되었습니다. memberId={}", memberId);
            throw new AuthException(AuthErrorCode.TEST_TOKEN_ISSUE_UNAUTHORIZED);
        }

        Member member = memberRepository.findByIdAndStatusAndDeletedAtIsNull(memberId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        String accessToken = jwtUtil.createTestAccessToken(new AuthMember(member));
        log.info("테스트 토큰을 발급했습니다. memberId={}", memberId);
        return accessToken;
    }
}
