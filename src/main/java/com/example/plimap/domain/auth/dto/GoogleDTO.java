package com.example.plimap.domain.auth.dto;

import com.example.plimap.domain.auth.enums.AuthProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GoogleDTO implements OAuthDTO {

    private final String providerSubject;
    private final String email;
    private final String nickname;

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
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
