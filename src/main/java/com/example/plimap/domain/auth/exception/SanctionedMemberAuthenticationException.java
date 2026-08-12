package com.example.plimap.domain.auth.exception;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.report.enums.ReportCategory;
import java.time.Instant;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

// 정지(SUSPENDED) 중이거나 벌점으로 자동 탈퇴(WITHDRAWN/PENALTY)된 회원이 로그인을 시도할 때 던진다.
@Getter
public class SanctionedMemberAuthenticationException extends AuthenticationException {

    private final MemberStatus status;
    private final ReportCategory reasonCategory;
    private final String reasonDetail;
    private final Instant suspendedUntil;

    public SanctionedMemberAuthenticationException(Member member) {
        super("정지 또는 탈퇴 처리된 계정은 로그인할 수 없습니다.");
        this.status = member.getStatus();
        this.reasonCategory = member.getLastPenaltyCategory();
        this.reasonDetail = member.getLastPenaltyDetail();
        this.suspendedUntil = member.getSuspendedUntil();
    }
}
