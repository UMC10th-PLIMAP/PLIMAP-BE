package com.example.plimap.domain.member.service.command.impl;

import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
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

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberQueryService memberQueryService = mock(MemberQueryService.class);

    private MemberCommandServiceImpl memberCommandService;

    @BeforeEach
    void setUp() {
        memberCommandService = new MemberCommandServiceImpl(memberRepository, memberQueryService);
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
    void 프로필_수정_시_존재하지_않는_회원이면_예외가_발생한다() {
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberCommandService.updateProfile(MEMBER_ID, updateProfile("새닉네임", null, null, null)))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 프로필_수정_시_닉네임을_다른_값으로_변경하는데_이미_사용중이면_예외가_발생한다() {
        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn("기존닉네임");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberQueryService.isNicknameAvailable("새닉네임")).thenReturn(false);

        assertThatThrownBy(() -> memberCommandService.updateProfile(MEMBER_ID, updateProfile("새닉네임", null, null, null)))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.NICKNAME_DUPLICATE));

        verify(member, never()).updateProfile(any(), any(), any(), any());
    }

    @Test
    void 닉네임을_기존과_대소문자만_다르게_요청하면_중복_검사_없이_수정된다() {
        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn("plimap");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        memberCommandService.updateProfile(MEMBER_ID, updateProfile("PLIMAP", null, null, null));

        verify(memberQueryService, never()).isNicknameAvailable(any());
        verify(member).updateProfile("PLIMAP", null, null, null);
    }

    @Test
    void 닉네임을_보내지_않으면_중복_검사_없이_나머지_필드만_수정된다() {
        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn("기존닉네임");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));

        memberCommandService.updateProfile(MEMBER_ID, updateProfile(null, "새이름", "새소개", null));

        verify(memberQueryService, never()).isNicknameAvailable(any());
        verify(member).updateProfile(null, "새이름", "새소개", null);
    }

    @Test
    void 정상_요청이면_프로필을_수정하고_회원을_반환한다() {
        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn("기존닉네임");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberQueryService.isNicknameAvailable("새닉네임")).thenReturn(true);

        MemberReqDTO.UpdateProfile request = updateProfile("새닉네임", "새이름", "새소개", "profile/1/new.jpg");
        Member result = memberCommandService.updateProfile(MEMBER_ID, request);

        assertThat(result).isSameAs(member);
        verify(member).updateProfile("새닉네임", "새이름", "새소개", "profile/1/new.jpg");
        verify(memberRepository).flush();
    }

    @Test
    void 프로필_수정_중_동시_요청으로_유니크_제약이_깨지면_닉네임_중복_예외로_변환한다() {
        Member member = mock(Member.class);
        when(member.getNickname()).thenReturn("기존닉네임");
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(memberQueryService.isNicknameAvailable("새닉네임")).thenReturn(true);
        doThrow(new DataIntegrityViolationException("duplicate")).when(memberRepository).flush();

        assertThatThrownBy(() -> memberCommandService.updateProfile(MEMBER_ID, updateProfile("새닉네임", null, null, null)))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.NICKNAME_DUPLICATE));
    }

    private MemberReqDTO.Onboarding onboarding(String nickname) {
        MemberReqDTO.Onboarding request = new MemberReqDTO.Onboarding();
        ReflectionTestUtils.setField(request, "nickname", nickname);
        ReflectionTestUtils.setField(request, "profileImageObjectKey", "profile/1/key.jpg");
        return request;
    }

    private MemberReqDTO.UpdateProfile updateProfile(String nickname, String name, String introduction, String profileImageObjectKey) {
        return new MemberReqDTO.UpdateProfile(nickname, name, introduction, profileImageObjectKey);
    }
}
