package com.example.plimap.global.security;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MemberStatusInterceptorTest {

    private static final Long MEMBER_ID = 1L;

    private final MemberCommandService memberCommandService = mock(MemberCommandService.class);
    private final MemberStatusInterceptor interceptor = new MemberStatusInterceptor(memberCommandService);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증되지_않은_요청은_통과한다() {
        MockHttpServletRequest request = request("GET", "/api/v1/pins/map");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void ACTIVE_회원은_통과한다() {
        authenticateAs(member(MemberStatus.ACTIVE, null));
        MockHttpServletRequest request = request("GET", "/api/v1/pins/1");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void 정지_만료_전이면_예외가_발생하고_해제를_호출하지_않는다() {
        Instant suspendedUntil = Instant.now().plusSeconds(3600);
        authenticateAs(member(MemberStatus.SUSPENDED, suspendedUntil));
        MockHttpServletRequest request = request("GET", "/api/v1/pins/1");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.SUSPENDED));

        verify(memberCommandService, never()).liftSuspension(MEMBER_ID);
    }

    @Test
    void 정지_만료_후이면_자동으로_해제되고_통과한다() {
        Instant suspendedUntil = Instant.now().minusSeconds(60);
        Member member = member(MemberStatus.SUSPENDED, suspendedUntil);
        authenticateAs(member);
        MockHttpServletRequest request = request("GET", "/api/v1/pins/1");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        verify(memberCommandService).liftSuspension(MEMBER_ID);
    }

    @Test
    void WITHDRAWN_회원은_차단된다() {
        authenticateAs(member(MemberStatus.WITHDRAWN, null));
        MockHttpServletRequest request = request("GET", "/api/v1/pins/1");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.WITHDRAWN));
    }

    @Test
    void 정지_중에도_로그아웃은_허용된다() {
        authenticateAs(member(MemberStatus.SUSPENDED, Instant.now().plusSeconds(3600)));
        MockHttpServletRequest request = request("DELETE", "/api/v1/auth/logout");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
    }

    @Test
    void 탈퇴_중에도_내_프로필_조회와_자발적_탈퇴는_허용된다() {
        Member withdrawn = member(MemberStatus.WITHDRAWN, null);

        authenticateAs(withdrawn);
        assertThat(interceptor.preHandle(request("GET", "/api/v1/members/me"), response, new Object())).isTrue();

        authenticateAs(withdrawn);
        assertThat(interceptor.preHandle(request("DELETE", "/api/v1/members/me"), response, new Object())).isTrue();
    }

    @Test
    void 정지_중에는_프로필_수정은_허용되지_않는다() {
        authenticateAs(member(MemberStatus.SUSPENDED, Instant.now().plusSeconds(3600)));
        MockHttpServletRequest request = request("PATCH", "/api/v1/members/me");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(MemberException.class);
    }

    private Member member(MemberStatus status, Instant suspendedUntil) {
        Member member = Member.builder().nickname("예림").build();
        ReflectionTestUtils.setField(member, "id", MEMBER_ID);
        ReflectionTestUtils.setField(member, "status", status);
        ReflectionTestUtils.setField(member, "suspendedUntil", suspendedUntil);
        return member;
    }

    private void authenticateAs(Member member) {
        AuthMember authMember = new AuthMember(member);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authMember, null, authMember.getAuthorities()));
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setRequestURI(uri);
        return request;
    }
}
