package com.example.plimap.domain.auth.exception;

import org.springframework.security.core.AuthenticationException;

// 벌점 4점 누적으로 자동 탈퇴된 회원이 같은 소셜 계정으로 재가입을 시도할 때 던진다.
public class WithdrawnMemberAuthenticationException extends AuthenticationException {

    public WithdrawnMemberAuthenticationException() {
        super("벌점으로 자동 탈퇴된 회원은 같은 소셜 계정으로 재가입할 수 없습니다.");
    }
}
