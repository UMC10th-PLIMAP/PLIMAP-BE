package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.dto.KakaoDTO;
import com.example.plimap.domain.auth.entity.SocialAccount;
import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.auth.exception.SanctionedMemberAuthenticationException;
import com.example.plimap.domain.auth.repository.SocialAccountRepository;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.WithdrawalReason;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void 벌점으로_자동_탈퇴된_회원이_같은_소셜_계정으로_재로그인하면_예외가_발생한다() {
        Member member = memberRepository.saveAndFlush(Member.builder().build());
        ReflectionTestUtils.setField(member, "status", MemberStatus.WITHDRAWN);
        ReflectionTestUtils.setField(member, "withdrawalReason", WithdrawalReason.PENALTY);
        memberRepository.saveAndFlush(member);
        socialAccountRepository.saveAndFlush(
                SocialAccount.create(member, AuthProvider.KAKAO, "banned-subject", "banned@example.com"));

        assertThatThrownBy(() -> customOAuthService.resolveMember(
                AuthProvider.KAKAO, new KakaoDTO("banned-subject", "banned@example.com", "예림")))
                .isInstanceOf(SanctionedMemberAuthenticationException.class);
    }

    @Test
    void 정지_기간이_아직_남은_회원이_로그인하면_예외가_발생한다() {
        Member member = memberRepository.saveAndFlush(Member.builder().build());
        ReflectionTestUtils.setField(member, "status", MemberStatus.SUSPENDED);
        ReflectionTestUtils.setField(member, "suspendedUntil", Instant.now().plusSeconds(3600));
        memberRepository.saveAndFlush(member);
        socialAccountRepository.saveAndFlush(
                SocialAccount.create(member, AuthProvider.KAKAO, "suspended-subject", "suspended@example.com"));

        assertThatThrownBy(() -> customOAuthService.resolveMember(
                AuthProvider.KAKAO, new KakaoDTO("suspended-subject", "suspended@example.com", "예림")))
                .isInstanceOf(SanctionedMemberAuthenticationException.class);
    }

    @Test
    void 정지_기간이_이미_지난_회원은_로그인_시점에_자동으로_해제되고_로그인에_성공한다() {
        Member member = memberRepository.saveAndFlush(Member.builder().build());
        ReflectionTestUtils.setField(member, "status", MemberStatus.SUSPENDED);
        ReflectionTestUtils.setField(member, "suspendedUntil", Instant.now().minusSeconds(1));
        memberRepository.saveAndFlush(member);
        socialAccountRepository.saveAndFlush(
                SocialAccount.create(member, AuthProvider.KAKAO, "expired-suspension-subject", "expired@example.com"));

        Member resolvedMember = customOAuthService.resolveMember(
                AuthProvider.KAKAO, new KakaoDTO("expired-suspension-subject", "expired@example.com", "예림"));

        assertThat(resolvedMember.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(resolvedMember.getSuspendedUntil()).isNull();
    }
}
