package com.example.plimap.domain.member;

import java.util.Locale;
import java.util.Set;

// 이 이메일로 소셜 로그인하면 가입/로그인 시점에 자동으로 관리자 권한을 부여한다.
public final class AdminEmailPolicy {

    private static final Set<String> ADMIN_EMAILS = Set.of("plimapteam@gmail.com");

    public static boolean isAdminEmail(String email) {
        return email != null && ADMIN_EMAILS.contains(email.toLowerCase(Locale.ROOT));
    }

    private AdminEmailPolicy() {
    }
}
