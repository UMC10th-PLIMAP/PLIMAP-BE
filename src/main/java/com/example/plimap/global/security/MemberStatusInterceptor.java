package com.example.plimap.global.security;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * SUSPENDED/WITHDRAWN 회원의 요청을 차단한다. JwtAuthFilter는 상태와 무관하게 인증만 수행하므로,
 * 여기서 상태별 분기(정지 만료일 안내, lazy 자동 해제, 탈퇴 차단)를 전담한다.
 */
@RequiredArgsConstructor
public class MemberStatusInterceptor implements HandlerInterceptor {

    private static final DateTimeFormatter SUSPENDED_UNTIL_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("Asia/Seoul"));

    private static final Set<AllowedRequest> ALLOWED_DURING_RESTRICTION = Set.of(
            new AllowedRequest(HttpMethod.GET, "/api/v1/auth/csrf"),
            new AllowedRequest(HttpMethod.DELETE, "/api/v1/auth/logout"),
            new AllowedRequest(HttpMethod.GET, "/api/v1/members/me"),
            new AllowedRequest(HttpMethod.DELETE, "/api/v1/members/me")
    );

    private final MemberCommandService memberCommandService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthMember authMember)) {
            return true;
        }

        Member member = authMember.getMember();
        if (member.getStatus() == MemberStatus.ACTIVE) {
            return true;
        }
        if (isAllowedDuringRestriction(request)) {
            return true;
        }

        if (member.getStatus() == MemberStatus.SUSPENDED) {
            if (member.isSuspensionExpired()) {
                memberCommandService.liftSuspension(member.getId());
                member.liftSuspension();
                return true;
            }
            throw new MemberException(
                    MemberErrorCode.SUSPENDED,
                    "관리자에 의해 %s까지 정지된 계정입니다.".formatted(SUSPENDED_UNTIL_FORMAT.format(member.getSuspendedUntil()))
            );
        }

        throw new MemberException(MemberErrorCode.WITHDRAWN);
    }

    private boolean isAllowedDuringRestriction(HttpServletRequest request) {
        AllowedRequest current = new AllowedRequest(HttpMethod.valueOf(request.getMethod()), request.getRequestURI());
        return ALLOWED_DURING_RESTRICTION.contains(current);
    }

    private record AllowedRequest(HttpMethod method, String uri) {
    }
}
