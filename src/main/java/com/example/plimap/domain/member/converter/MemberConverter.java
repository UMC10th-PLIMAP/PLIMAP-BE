package com.example.plimap.domain.member.converter;

import com.example.plimap.domain.auth.dto.OAuthDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;

public class MemberConverter {

    public static Member toMember(OAuthDTO dto) {
        return Member.builder()
                .build();
    }

    public static MemberResDTO.Login toLogin(String accessToken) {
        return MemberResDTO.Login.builder()
                .accessToken(accessToken)
                .build();
    }
}
