package com.example.plimap.domain.auth.service.command.impl;

import com.example.plimap.domain.auth.config.TestTokenProperties;
import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.auth.exception.AuthErrorCode;
import com.example.plimap.domain.auth.exception.AuthException;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.global.security.JwtUtil;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TestTokenCommandServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final String ISSUE_KEY = "3B7YhG0A5czm0k0xGkTxYJofXxGtDZQp1yAp3_9XgS8";

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);

    @Test
    void 올바른_발급_키와_활성_회원이면_테스트_토큰을_발급한다() {
        TestTokenCommandServiceImpl service = devService();
        Member member = Member.builder().build();
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(MEMBER_ID, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));
        when(jwtUtil.createTestAccessToken(any(AuthMember.class)))
                .thenReturn("test-access-token");

        String accessToken = service.issueTestToken(MEMBER_ID, ISSUE_KEY);

        assertThat(accessToken).isEqualTo("test-access-token");
        verify(memberRepository).findByIdAndStatusAndDeletedAtIsNull(MEMBER_ID, MemberStatus.ACTIVE);
        verify(jwtUtil).createTestAccessToken(any(AuthMember.class));
    }

    @Test
    void 발급_키가_누락되면_회원_조회_전에_거부한다() {
        TestTokenCommandServiceImpl service = devService();

        assertThatThrownBy(() -> service.issueTestToken(MEMBER_ID, null))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.TEST_TOKEN_ISSUE_UNAUTHORIZED));

        verifyNoInteractions(memberRepository, jwtUtil);
    }

    @Test
    void 발급_키가_일치하지_않으면_회원_조회_전에_거부한다() {
        TestTokenCommandServiceImpl service = devService();

        assertThatThrownBy(() -> service.issueTestToken(MEMBER_ID, ISSUE_KEY + "x"))
                .isInstanceOf(AuthException.class)
                .satisfies(exception -> assertThat(((AuthException) exception).getErrorCode())
                        .isEqualTo(AuthErrorCode.TEST_TOKEN_ISSUE_UNAUTHORIZED));

        verifyNoInteractions(memberRepository, jwtUtil);
    }

    @Test
    void 활성_상태가_아니거나_존재하지_않는_회원이면_발급하지_않는다() {
        TestTokenCommandServiceImpl service = devService();
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(MEMBER_ID, MemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.issueTestToken(MEMBER_ID, ISSUE_KEY))
                .isInstanceOf(MemberException.class)
                .satisfies(exception -> assertThat(((MemberException) exception).getErrorCode())
                        .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));

        verify(jwtUtil, never()).createTestAccessToken(any(AuthMember.class));
    }

    @Test
    void Local에서는_발급_키_없이도_활성_회원_토큰을_발급한다() {
        TestTokenProperties localProperties = new TestTokenProperties(false, null);
        TestTokenCommandServiceImpl service =
                new TestTokenCommandServiceImpl(localProperties, memberRepository, jwtUtil);
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(MEMBER_ID, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(Member.builder().build()));
        when(jwtUtil.createTestAccessToken(any(AuthMember.class)))
                .thenReturn("local-test-access-token");

        String accessToken = service.issueTestToken(MEMBER_ID, null);

        assertThat(accessToken).isEqualTo("local-test-access-token");
    }

    private TestTokenCommandServiceImpl devService() {
        return new TestTokenCommandServiceImpl(
                new TestTokenProperties(true, ISSUE_KEY),
                memberRepository,
                jwtUtil
        );
    }
}
