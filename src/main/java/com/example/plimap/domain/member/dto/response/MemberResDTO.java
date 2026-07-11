package com.example.plimap.domain.member.dto.response;

import lombok.Builder;
import lombok.Getter;

public class MemberResDTO {

    @Getter
    @Builder
    public static class Login {
        private String accessToken;
    }
}
