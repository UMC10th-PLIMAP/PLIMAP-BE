package com.example.plimap.domain.auth.service.query.impl;

import com.example.plimap.domain.auth.entity.SocialAccount;
import com.example.plimap.domain.auth.enums.AuthProvider;
import com.example.plimap.domain.auth.repository.SocialAccountRepository;
import com.example.plimap.domain.member.entity.Member;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthQueryServiceImplTest {

    private final SocialAccountRepository socialAccountRepository = mock(SocialAccountRepository.class);
    private final AuthQueryServiceImpl authQueryService = new AuthQueryServiceImpl(socialAccountRepository);

    @Test
    void memberId로_이메일을_조회한다() {
        SocialAccount socialAccount = socialAccount(1L, "kakao@example.com");
        when(socialAccountRepository.findByMember_IdInOrderByCreatedAtAsc(List.of(1L)))
                .thenReturn(List.of(socialAccount));

        Optional<String> result = authQueryService.findEmailByMemberId(1L);

        assertThat(result).contains("kakao@example.com");
    }

    @Test
    void 소셜계정이_없는_회원은_빈_Optional을_반환한다() {
        when(socialAccountRepository.findByMember_IdInOrderByCreatedAtAsc(List.of(1L)))
                .thenReturn(List.of());

        Optional<String> result = authQueryService.findEmailByMemberId(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void 여러_회원의_이메일을_한번에_조회한다() {
        when(socialAccountRepository.findByMember_IdInOrderByCreatedAtAsc(List.of(1L, 2L)))
                .thenReturn(List.of(socialAccount(1L, "a@example.com"), socialAccount(2L, "b@example.com")));

        Map<Long, String> result = authQueryService.findEmailsByMemberIds(List.of(1L, 2L));

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(1L, "a@example.com", 2L, "b@example.com"));
    }

    @Test
    void 한_회원이_여러_소셜계정을_가지면_가장_먼저_생성된_계정의_이메일을_사용한다() {
        when(socialAccountRepository.findByMember_IdInOrderByCreatedAtAsc(List.of(1L)))
                .thenReturn(List.of(socialAccount(1L, "first@example.com"), socialAccount(1L, "second@example.com")));

        Map<Long, String> result = authQueryService.findEmailsByMemberIds(List.of(1L));

        assertThat(result).containsExactly(Map.entry(1L, "first@example.com"));
    }

    @Test
    void memberIds가_비어있으면_조회하지_않고_빈_Map을_반환한다() {
        Map<Long, String> result = authQueryService.findEmailsByMemberIds(List.of());

        assertThat(result).isEmpty();
    }

    private SocialAccount socialAccount(Long memberId, String email) {
        Member member = Member.builder().build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return SocialAccount.create(member, AuthProvider.KAKAO, "subject-" + memberId, email);
    }
}
