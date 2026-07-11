package com.example.plimap.domain.auth.dto;

import com.example.plimap.domain.auth.enums.AuthProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KakaoDTO implements OAuthDTO {

    private final String providerSubject;
    private final String email;
    private final String nickname;

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.KAKAO;
    }

    @Override
    public String getProviderSubject() {
        return providerSubject;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getNickname() {
        return nickname;
    }
}
