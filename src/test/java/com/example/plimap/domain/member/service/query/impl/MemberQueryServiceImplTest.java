package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberQueryServiceImplTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final MemberQueryServiceImpl memberQueryService = new MemberQueryServiceImpl(memberRepository);

    @Test
    void 활성_회원을_조회한다() {
        // given
        Member member = Member.builder().nickname("회원").build();
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(member));

        // when
        Member result = memberQueryService.getActiveMember(1L);

        // then
        assertThat(result).isSameAs(member);
    }

    @Test
    void 존재하지_않거나_삭제된_회원은_조회할_수_없다() {
        // given
        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberQueryService.getActiveMember(1L))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }

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

    @Test
    void 닉네임_검증을_모두_통과하면_실패_사유가_없다() {
        when(memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull("예림")).thenReturn(false);

        assertThat(memberQueryService.checkNicknameFailReason("예림")).isNull();
    }

    @Test
    void 닉네임이_두_글자_미만이면_TOO_SHORT를_반환한다() {
        assertThat(memberQueryService.checkNicknameFailReason("예")).isEqualTo(NicknameCheckFailReason.TOO_SHORT);
    }

    @Test
    void 닉네임이_열_글자_초과이면_TOO_LONG을_반환한다() {
        assertThat(memberQueryService.checkNicknameFailReason("가나다라마바사아자차"))
                .isNull();
        assertThat(memberQueryService.checkNicknameFailReason("가나다라마바사아자차카"))
                .isEqualTo(NicknameCheckFailReason.TOO_LONG);
    }

    @Test
    void 닉네임에_공백이나_특수문자가_있으면_INVALID_FORMAT을_반환한다() {
        assertThat(memberQueryService.checkNicknameFailReason("예 림"))
                .isEqualTo(NicknameCheckFailReason.INVALID_FORMAT);
        assertThat(memberQueryService.checkNicknameFailReason("예림!"))
                .isEqualTo(NicknameCheckFailReason.INVALID_FORMAT);
    }

    @Test
    void 닉네임에_금칙어가_포함되면_FORBIDDEN_WORD를_반환한다() {
        assertThat(memberQueryService.checkNicknameFailReason("씨발닉네임"))
                .isEqualTo(NicknameCheckFailReason.FORBIDDEN_WORD);
    }

    @Test
    void 닉네임에_PLIMAP이_대소문자와_무관하게_포함되면_FORBIDDEN_WORD를_반환한다() {
        assertThat(memberQueryService.checkNicknameFailReason("PLIMAP운영진"))
                .isEqualTo(NicknameCheckFailReason.FORBIDDEN_WORD);
        assertThat(memberQueryService.checkNicknameFailReason("plimap운영진"))
                .isEqualTo(NicknameCheckFailReason.FORBIDDEN_WORD);
    }

    @Test
    void 닉네임에_플리맵운영자가_포함되면_FORBIDDEN_WORD를_반환한다() {
        assertThat(memberQueryService.checkNicknameFailReason("플리맵운영자임"))
                .isEqualTo(NicknameCheckFailReason.FORBIDDEN_WORD);
    }

    @Test
    void 닉네임이_이미_사용중이면_DUPLICATE를_반환한다() {
        when(memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull("예림")).thenReturn(true);

        assertThat(memberQueryService.checkNicknameFailReason("예림")).isEqualTo(NicknameCheckFailReason.DUPLICATE);
    }

    @Test
    void 길이_초과가_형식_오류보다_우선한다() {
        // 가나다라마바사아자차카! - 11자(길이 초과) + 특수문자(형식 오류) 동시 위반
        assertThat(memberQueryService.checkNicknameFailReason("가나다라마바사아자차카!"))
                .isEqualTo(NicknameCheckFailReason.TOO_LONG);
    }

    @Test
    void 형식_오류면_금칙어와_중복_여부는_확인하지_않는다() {
        // PLIMAP!(형식 오류)이 아니었다면 금칙어(PLIMAP)에도 걸렸을 닉네임
        assertThat(memberQueryService.checkNicknameFailReason("PLIMAP!"))
                .isEqualTo(NicknameCheckFailReason.INVALID_FORMAT);

        verify(memberRepository, never()).existsByNicknameIgnoreCaseAndDeletedAtIsNull(any());
    }

    @Test
    void 금칙어_포함이_중복_여부보다_우선하고_중복_여부는_확인하지_않는다() {
        when(memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull("PLIMAP다")).thenReturn(true);

        assertThat(memberQueryService.checkNicknameFailReason("PLIMAP다"))
                .isEqualTo(NicknameCheckFailReason.FORBIDDEN_WORD);

        verify(memberRepository, never()).existsByNicknameIgnoreCaseAndDeletedAtIsNull(any());
    }
}
