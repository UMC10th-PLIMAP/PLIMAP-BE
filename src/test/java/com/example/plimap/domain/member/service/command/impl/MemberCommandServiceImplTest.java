package com.example.plimap.domain.member.service.command.impl;

import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.entity.MemberFollowId;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberFollowRepository;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberCommandServiceImplTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberFollowRepository memberFollowRepository = mock(MemberFollowRepository.class);
    private final MemberQueryService memberQueryService = mock(MemberQueryService.class);

    private MemberCommandServiceImpl memberCommandService;

    @BeforeEach
    void setUp() {
        memberCommandService = new MemberCommandServiceImpl(memberRepository, memberFollowRepository, memberQueryService);
    }

    @Test
    void 존재하지_않는_회원이면_예외가_발생한다() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCommandService.completeOnboarding(MEMBER_ID, onboarding("닉네임")))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 이미_온보딩을_완료한_회원이면_예외가_발생한다() {
        Member member = mock(Member.class);
        when(member.isOnboarded()).thenReturn(true);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> memberCommandService.completeOnboarding(MEMBER_ID, onboarding("닉네임")))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.ALREADY_ONBOARDED));

        verify(member, never()).completeOnboarding(any(), any());
    }

    @Test
    void 닉네임이_이미_사용중이면_예외가_발생한다() {
        Member member = mock(Member.class);
        when(member.isOnboarded()).thenReturn(false);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberQueryService.isNicknameAvailable("닉네임")).thenReturn(false);

        assertThatThrownBy(() -> memberCommandService.completeOnboarding(MEMBER_ID, onboarding("닉네임")))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.NICKNAME_DUPLICATE));

        verify(member, never()).completeOnboarding(any(), any());
    }

    @Test
    void 정상_요청이면_온보딩을_완료하고_회원을_반환한다() {
        Member member = mock(Member.class);
        when(member.isOnboarded()).thenReturn(false);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberQueryService.isNicknameAvailable("닉네임")).thenReturn(true);

        MemberReqDTO.Onboarding request = onboarding("닉네임");
        Member result = memberCommandService.completeOnboarding(MEMBER_ID, request);

        assertThat(result).isSameAs(member);
        verify(member).completeOnboarding("닉네임", request.getProfileImageObjectKey());
        verify(memberRepository).flush();
    }

    @Test
    void 동시_요청으로_유니크_제약이_깨지면_닉네임_중복_예외로_변환한다() {
        Member member = mock(Member.class);
        when(member.isOnboarded()).thenReturn(false);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberQueryService.isNicknameAvailable("닉네임")).thenReturn(true);
        doThrow(new DataIntegrityViolationException("duplicate")).when(memberRepository).flush();

        assertThatThrownBy(() -> memberCommandService.completeOnboarding(MEMBER_ID, onboarding("닉네임")))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.NICKNAME_DUPLICATE));
    }

    @Test
    void 자기_자신을_팔로우하면_예외가_발생한다() {
        assertThatThrownBy(() -> memberCommandService.follow(MEMBER_ID, MEMBER_ID))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.CANNOT_FOLLOW_SELF));

        verify(memberRepository, never()).findById(any());
    }

    @Test
    void 팔로우_대상_회원이_존재하지_않으면_예외가_발생한다() {
        Member follower = mock(Member.class);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(follower));
        when(memberRepository.findByIdAndDeletedAtIsNull(OTHER_MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCommandService.follow(MEMBER_ID, OTHER_MEMBER_ID))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));

        verify(memberFollowRepository, never()).existsById(any());
    }

    @Test
    void 이미_팔로우_중이면_예외가_발생한다() {
        Member follower = mock(Member.class);
        Member following = mock(Member.class);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(follower));
        when(memberRepository.findByIdAndDeletedAtIsNull(OTHER_MEMBER_ID)).thenReturn(Optional.of(following));
        when(memberFollowRepository.existsById(new MemberFollowId(MEMBER_ID, OTHER_MEMBER_ID))).thenReturn(true);

        assertThatThrownBy(() -> memberCommandService.follow(MEMBER_ID, OTHER_MEMBER_ID))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.ALREADY_FOLLOWING));

        verify(memberFollowRepository, never()).saveAndFlush(any());
    }

    @Test
    void 정상_요청이면_팔로우한다() {
        Member follower = mock(Member.class);
        when(follower.getId()).thenReturn(MEMBER_ID);
        Member following = mock(Member.class);
        when(following.getId()).thenReturn(OTHER_MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(follower));
        when(memberRepository.findByIdAndDeletedAtIsNull(OTHER_MEMBER_ID)).thenReturn(Optional.of(following));
        when(memberFollowRepository.existsById(new MemberFollowId(MEMBER_ID, OTHER_MEMBER_ID))).thenReturn(false);

        memberCommandService.follow(MEMBER_ID, OTHER_MEMBER_ID);

        verify(memberFollowRepository).saveAndFlush(any(MemberFollow.class));
    }

    @Test
    void 동시_요청으로_복합_PK가_깨지면_이미_팔로우_중_예외로_변환한다() {
        Member follower = mock(Member.class);
        when(follower.getId()).thenReturn(MEMBER_ID);
        Member following = mock(Member.class);
        when(following.getId()).thenReturn(OTHER_MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(follower));
        when(memberRepository.findByIdAndDeletedAtIsNull(OTHER_MEMBER_ID)).thenReturn(Optional.of(following));
        when(memberFollowRepository.existsById(new MemberFollowId(MEMBER_ID, OTHER_MEMBER_ID))).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate")).when(memberFollowRepository).saveAndFlush(any(MemberFollow.class));

        assertThatThrownBy(() -> memberCommandService.follow(MEMBER_ID, OTHER_MEMBER_ID))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.ALREADY_FOLLOWING));
    }

    private MemberReqDTO.Onboarding onboarding(String nickname) {
        MemberReqDTO.Onboarding request = new MemberReqDTO.Onboarding();
        ReflectionTestUtils.setField(request, "nickname", nickname);
        ReflectionTestUtils.setField(request, "profileImageObjectKey", "profile/1/key.jpg");
        return request;
    }
}
