package com.example.plimap.global.security;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.entity.Member;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtUtilTest {

    private static final String SECRET = "test-only-jwt-secret-key-do-not-use-in-production-0123456789";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET);

    @Test
    void 발급한_토큰마다_서로_다른_JTI를_가진다() {
        AuthMember authMember = authMember(1L);

        String token1 = jwtUtil.createAccessToken(authMember);
        String token2 = jwtUtil.createAccessToken(authMember);

        assertThat(jwtUtil.getJti(token1)).isNotBlank();
        assertThat(jwtUtil.getJti(token1)).isNotEqualTo(jwtUtil.getJti(token2));
    }

    @Test
    void 발급한_토큰에서_회원_ID를_추출한다() {
        String token = jwtUtil.createAccessToken(authMember(42L));

        assertThat(jwtUtil.getMemberId(token)).isEqualTo(42L);
    }

    @Test
    void 발급한_토큰은_남은_유효기간이_설정된_만료기간_이하이다() {
        String token = jwtUtil.createAccessToken(authMember(1L));

        Duration remaining = jwtUtil.getRemainingExpiry(token);

        assertThat(remaining).isPositive();
        assertThat(remaining).isLessThanOrEqualTo(jwtUtil.getAccessTokenExpiry());
    }

    @Test
    void 서명이_다른_토큰은_유효하지_않다() {
        String token = jwtUtil.createAccessToken(authMember(1L));
        JwtUtil otherJwtUtil = new JwtUtil("different-secret-key-0123456789-0123456789");

        assertThat(otherJwtUtil.isValid(token)).isFalse();
    }

    @Test
    void Access_Token과_Refresh_Token은_타입_클레임으로_서로_구분된다() {
        String accessToken = jwtUtil.createAccessToken(authMember(1L));
        String refreshToken = jwtUtil.createRefreshToken(authMember(1L));

        assertThat(jwtUtil.isAccessToken(accessToken)).isTrue();
        assertThat(jwtUtil.isRefreshToken(accessToken)).isFalse();

        assertThat(jwtUtil.isAccessToken(refreshToken)).isFalse();
        assertThat(jwtUtil.isRefreshToken(refreshToken)).isTrue();
    }

    @Test
    void 테스트_액세스_토큰은_1시간_동안_유효하다() {
        String token = jwtUtil.createTestAccessToken(authMember(1L));

        Duration remaining = jwtUtil.getRemainingExpiry(token);

        assertThat(jwtUtil.isAccessToken(token)).isTrue();
        assertThat(remaining).isPositive();
        assertThat(remaining)
                .isLessThanOrEqualTo(jwtUtil.getTestAccessTokenExpiry())
                .isGreaterThan(jwtUtil.getTestAccessTokenExpiry().minusSeconds(5));
    }

    private AuthMember authMember(Long memberId) {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(memberId);
        return new AuthMember(member);
    }
}
