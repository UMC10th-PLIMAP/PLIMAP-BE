package com.example.plimap.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenResolverTest {

    @Test
    void Authorization_헤더의_Bearer_토큰을_우선적으로_사용한다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer header-token");
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("accessToken", "cookie-token")});

        assertThat(TokenResolver.resolve(request)).isEqualTo("header-token");
    }

    @Test
    void Bearer_스킴은_대소문자를_구분하지_않는다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("bearer header-token");

        assertThat(TokenResolver.resolve(request)).isEqualTo("header-token");
    }

    @Test
    void 헤더가_없으면_accessToken_쿠키를_사용한다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("accessToken", "cookie-token")});

        assertThat(TokenResolver.resolve(request)).isEqualTo("cookie-token");
    }

    @Test
    void 헤더와_쿠키가_모두_없으면_null을_반환한다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        assertThat(TokenResolver.resolve(request)).isNull();
    }
}
