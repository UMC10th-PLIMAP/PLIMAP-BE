package com.example.plimap.domain.admin.dto.response;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberRole;

public class AdminResDTO {

    public record Me(
            Long id,
            String nickname,
            MemberRole role
    ) {
        public static Me from(Member member) {
            return new Me(member.getId(), member.getNickname(), member.getRole());
        }
    }
}
