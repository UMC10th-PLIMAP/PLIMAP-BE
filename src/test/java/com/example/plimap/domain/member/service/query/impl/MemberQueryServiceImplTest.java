package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberQueryServiceImplTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberQueryServiceImpl memberQueryService = new MemberQueryServiceImpl(memberRepository);

    @Test
    void 닉네임이_이미_사용중이면_사용_불가능하다() {
        when(memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull("닉네임")).thenReturn(true);

        assertThat(memberQueryService.isNicknameAvailable("닉네임")).isFalse();
    }

    @Test
    void 닉네임이_사용중이_아니면_사용_가능하다() {
        when(memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull("닉네임")).thenReturn(false);

        assertThat(memberQueryService.isNicknameAvailable("닉네임")).isTrue();
    }
}
