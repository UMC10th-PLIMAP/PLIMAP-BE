package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.dto.KakaoDTO;
import com.example.plimap.domain.auth.entity.SocialAccount;
import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.auth.repository.SocialAccountRepository;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
class CustomOAuthServiceIntegrationTest {

    @Autowired
    private CustomOAuthService customOAuthService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Test
    void 기존_회원의_LAZY_프록시는_트랜잭션_종료_후에도_isOnboarded_조회가_가능하다() {
        Member member = memberRepository.saveAndFlush(Member.builder().build());
        member.completeOnboarding("예림");
        memberRepository.saveAndFlush(member);
        socialAccountRepository.saveAndFlush(
                SocialAccount.create(member, AuthProvider.KAKAO, "kakao-subject", "test@example.com"));

        // resolveMember()는 @Transactional이라 이 호출이 끝나면 세션이 닫힌다.
        // 픽스가 없으면 SocialAccount.member(LAZY) 프록시가 여기서 이미 초기화 안 된 채로 반환돼
        // 아래 isOnboarded() 호출에서 LazyInitializationException이 터진다.
        Member resolvedMember = customOAuthService.resolveMember(
                AuthProvider.KAKAO, new KakaoDTO("kakao-subject", "test@example.com", "예림"));

        assertThatCode(resolvedMember::isOnboarded).doesNotThrowAnyException();
        assertThat(resolvedMember.isOnboarded()).isTrue();
    }
}
