package com.example.plimap.domain.member.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.member.enums.MemberRole;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    void 이름에_빈_문자열을_보내면_null로_삭제된다() {
        // given
        Member member = Member.builder()
                .nickname("닉네임")
                .name("이예림")
                .joinProvider(AuthProvider.KAKAO)
                .role(MemberRole.USER)
                .build();

        // when
        member.updateProfile(null, "", null);

        // then
        assertThat(member.getName()).isNull();
    }

    @Test
    void 이름에_null을_보내면_기존_값이_유지된다() {
        // given
        Member member = Member.builder()
                .nickname("닉네임")
                .name("이예림")
                .joinProvider(AuthProvider.KAKAO)
                .role(MemberRole.USER)
                .build();

        // when
        member.updateProfile(null, null, null);

        // then
        assertThat(member.getName()).isEqualTo("이예림");
    }

    @Test
    void 이름에_값을_보내면_그_값으로_변경된다() {
        // given
        Member member = Member.builder()
                .nickname("닉네임")
                .name("이예림")
                .joinProvider(AuthProvider.KAKAO)
                .role(MemberRole.USER)
                .build();

        // when
        member.updateProfile(null, "새이름", null);

        // then
        assertThat(member.getName()).isEqualTo("새이름");
    }
}
