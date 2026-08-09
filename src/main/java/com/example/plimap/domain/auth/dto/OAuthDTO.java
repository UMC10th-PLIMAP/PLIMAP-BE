package com.example.plimap.domain.auth.dto;

import com.example.plimap.domain.auth.enums.AuthProvider;

public interface OAuthDTO {

    AuthProvider getProvider();

    String getProviderSubject();

    String getEmail();

    String getNickname();
}
