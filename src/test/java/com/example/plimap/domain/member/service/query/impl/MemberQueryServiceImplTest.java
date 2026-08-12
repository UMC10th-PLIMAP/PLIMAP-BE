package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollowId;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberFollowRepository;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.repository.query.MemberFollowRow;
import com.example.plimap.domain.member.repository.query.MemberQueryRepository;
import com.example.plimap.domain.member.repository.query.MemberSearchRow;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
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
    private final MemberFollowRepository memberFollowRepository = mock(MemberFollowRepository.class);
    private final MemberQueryRepository memberQueryRepository = mock(MemberQueryRepository.class);
    private final ProfileImageStorage profileImageStorage = mock(ProfileImageStorage.class);
    private final PinQueryService pinQueryService = mock(PinQueryService.class);
    private final MemberQueryServiceImpl memberQueryService =
            new MemberQueryServiceImpl(memberRepository, memberFollowRepository, memberQueryRepository, profileImageStorage, pinQueryService);

    @Test
    void 활성_회원을_조회한다() {
        // given
        Member member = Member.builder().nickname("회원").build();
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));

        // when
        Member result = memberQueryService.getActiveMember(1L);

        // then
        assertThat(result).isSameAs(member);
        verify(memberRepository).findByIdAndStatusAndDeletedAtIsNull(1L, MemberStatus.ACTIVE);
    }

    @Test
    void 존재하지_않거나_비활성_또는_삭제된_회원은_조회할_수_없다() {
        // given
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L, MemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

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
    void 벌점_닉네임_후보_풀에_미사용_값이_있으면_그중_하나를_반환한다() {
        // given
        when(memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull(any())).thenReturn(false);

        // when
        String result = memberQueryService.pickAvailablePenaltyNickname();

        // then
        assertThat(result).isNotBlank();
    }

    @Test
    void 벌점_닉네임_후보_풀이_모두_소진되면_폴백_후보로_대체한다() {
        // given: 접미사 없는 풀 후보는 전부 사용 중이고, 숫자 접미사가 붙은 폴백 후보만 사용 가능하다.
        when(memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull(any()))
                .thenAnswer(invocation -> {
                    String nickname = invocation.getArgument(0);
                    return !Character.isDigit(nickname.charAt(nickname.length() - 1));
                });

        // when
        String result = memberQueryService.pickAvailablePenaltyNickname();

        // then
        assertThat(result).matches(".+\\d+$");
    }

    @Test
    void 벌점_닉네임_후보_풀과_폴백_공간이_모두_소진되면_예외가_발생한다() {
        // given
        when(memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull(any())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> memberQueryService.pickAvailablePenaltyNickname())
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode())
                                .isEqualTo(MemberErrorCode.PENALTY_NICKNAME_POOL_EXHAUSTED));
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
    void 닉네임에_플리맵사용자가_포함되면_FORBIDDEN_WORD를_반환한다() {
        assertThat(memberQueryService.checkNicknameFailReason("플리맵사용자임"))
                .isEqualTo(NicknameCheckFailReason.FORBIDDEN_WORD);
    }

    @Test
    void isNicknameForbidden은_금칙어가_포함된_닉네임에_대해_true를_반환한다() {
        assertThat(memberQueryService.isNicknameForbidden("플리맵사용자임")).isTrue();
        assertThat(memberQueryService.isNicknameForbidden("plimap운영진")).isTrue();
    }

    @Test
    void isNicknameForbidden은_금칙어가_없는_닉네임에_대해_false를_반환한다() {
        assertThat(memberQueryService.isNicknameForbidden("예림")).isFalse();
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

    @Test
    void 내_프로필을_팔로워_팔로잉_수와_함께_조회한다() {
        // given
        Member member = Member.builder()
                .nickname("예림")
                .name("이예림")
                .introduction("소개")
                .profileImageObjectKey("key")
                .build();
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));
        when(memberFollowRepository.countByIdFollowingId(1L)).thenReturn(3L);
        when(memberFollowRepository.countByIdFollowerId(1L)).thenReturn(5L);
        when(pinQueryService.countPinsByMemberId(1L)).thenReturn(9L);
        when(profileImageStorage.getPublicUrlOrNull("key")).thenReturn("https://example.com/key");

        // when
        MemberResDTO.MyProfile result = memberQueryService.getMyProfile(1L);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.nickname()).isEqualTo("예림");
        assertThat(result.followerCount()).isEqualTo(3L);
        assertThat(result.followingCount()).isEqualTo(5L);
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/key");
        assertThat(result.pinCount()).isEqualTo(9L);
    }

    @Test
    void 팔로워나_팔로잉이_없으면_0을_반환한다() {
        // given
        Member member = Member.builder().nickname("예림").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));
        when(memberFollowRepository.countByIdFollowingId(1L)).thenReturn(0L);
        when(memberFollowRepository.countByIdFollowerId(1L)).thenReturn(0L);

        // when
        MemberResDTO.MyProfile result = memberQueryService.getMyProfile(1L);

        // then
        assertThat(result.followerCount()).isZero();
        assertThat(result.followingCount()).isZero();
    }

    @Test
    void 작성한_핀이_없으면_내_프로필의_pinCount는_0이다() {
        // given
        Member member = Member.builder().nickname("예림").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L, MemberStatus.ACTIVE))
                .thenReturn(Optional.of(member));
        when(pinQueryService.countPinsByMemberId(1L)).thenReturn(0L);

        // when
        MemberResDTO.MyProfile result = memberQueryService.getMyProfile(1L);

        // then
        assertThat(result.pinCount()).isZero();
    }

    @Test
    void 존재하지_않는_회원의_프로필은_조회할_수_없다() {
        // given
        when(memberRepository.findByIdAndStatusAndDeletedAtIsNull(1L, MemberStatus.ACTIVE))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberQueryService.getMyProfile(1L))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 다른_회원의_프로필을_팔로우_수와_함께_조회한다() {
        // given
        Member member = Member.builder().nickname("상대방").profileImageObjectKey("key").build();
        ReflectionTestUtils.setField(member, "id", 2L);
        when(memberQueryRepository.findVisibleActiveMember(2L, 1L))
                .thenReturn(Optional.of(member));
        when(memberFollowRepository.countByIdFollowingId(2L)).thenReturn(3L);
        when(memberFollowRepository.countByIdFollowerId(2L)).thenReturn(5L);
        when(memberFollowRepository.existsById(new MemberFollowId(1L, 2L))).thenReturn(false);
        when(pinQueryService.countPinsByMemberId(2L)).thenReturn(7L);
        when(profileImageStorage.getPublicUrlOrNull("key")).thenReturn("https://example.com/key");

        // when
        MemberResDTO.OtherProfile result = memberQueryService.getOtherProfile(1L, 2L);

        // then
        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.nickname()).isEqualTo("상대방");
        assertThat(result.followerCount()).isEqualTo(3L);
        assertThat(result.followingCount()).isEqualTo(5L);
        assertThat(result.profileImageUrl()).isEqualTo("https://example.com/key");
        assertThat(result.pinCount()).isEqualTo(7L);
    }

    @Test
    void 다른_회원이_작성한_핀이_없으면_pinCount는_0이다() {
        // given
        Member member = Member.builder().nickname("상대방").build();
        ReflectionTestUtils.setField(member, "id", 2L);
        when(memberQueryRepository.findVisibleActiveMember(2L, 1L))
                .thenReturn(Optional.of(member));
        when(pinQueryService.countPinsByMemberId(2L)).thenReturn(0L);

        // when
        MemberResDTO.OtherProfile result = memberQueryService.getOtherProfile(1L, 2L);

        // then
        assertThat(result.pinCount()).isZero();
    }

    @Test
    void 팔로우_중인_회원이면_isFollowing이_true다() {
        // given
        Member member = Member.builder().nickname("상대방").build();
        ReflectionTestUtils.setField(member, "id", 2L);
        when(memberQueryRepository.findVisibleActiveMember(2L, 1L))
                .thenReturn(Optional.of(member));
        when(memberFollowRepository.existsById(new MemberFollowId(1L, 2L))).thenReturn(true);

        // when
        MemberResDTO.OtherProfile result = memberQueryService.getOtherProfile(1L, 2L);

        // then
        assertThat(result.isFollowing()).isTrue();
    }

    @Test
    void 팔로우_중이_아니면_isFollowing이_false다() {
        // given
        Member member = Member.builder().nickname("상대방").build();
        ReflectionTestUtils.setField(member, "id", 2L);
        when(memberQueryRepository.findVisibleActiveMember(2L, 1L))
                .thenReturn(Optional.of(member));
        when(memberFollowRepository.existsById(new MemberFollowId(1L, 2L))).thenReturn(false);

        // when
        MemberResDTO.OtherProfile result = memberQueryService.getOtherProfile(1L, 2L);

        // then
        assertThat(result.isFollowing()).isFalse();
    }

    @Test
    void 상대가_나를_팔로우_중이면_isFollowingViewer가_true다() {
        // given
        Member member = Member.builder().nickname("상대방").build();
        ReflectionTestUtils.setField(member, "id", 2L);
        when(memberQueryRepository.findVisibleActiveMember(2L, 1L))
                .thenReturn(Optional.of(member));
        when(memberFollowRepository.existsById(new MemberFollowId(1L, 2L))).thenReturn(false);
        when(memberFollowRepository.existsById(new MemberFollowId(2L, 1L))).thenReturn(true);

        // when
        MemberResDTO.OtherProfile result = memberQueryService.getOtherProfile(1L, 2L);

        // then
        assertThat(result.isFollowing()).isFalse();
        assertThat(result.isFollowingViewer()).isTrue();
    }

    @Test
    void 상대가_나를_팔로우_중이_아니면_isFollowingViewer가_false다() {
        // given
        Member member = Member.builder().nickname("상대방").build();
        ReflectionTestUtils.setField(member, "id", 2L);
        when(memberQueryRepository.findVisibleActiveMember(2L, 1L))
                .thenReturn(Optional.of(member));
        when(memberFollowRepository.existsById(new MemberFollowId(2L, 1L))).thenReturn(false);

        // when
        MemberResDTO.OtherProfile result = memberQueryService.getOtherProfile(1L, 2L);

        // then
        assertThat(result.isFollowingViewer()).isFalse();
    }

    @Test
    void 맞팔로우_상태면_isFollowing과_isFollowingViewer가_모두_true다() {
        // given
        Member member = Member.builder().nickname("상대방").build();
        ReflectionTestUtils.setField(member, "id", 2L);
        when(memberQueryRepository.findVisibleActiveMember(2L, 1L))
                .thenReturn(Optional.of(member));
        when(memberFollowRepository.existsById(new MemberFollowId(1L, 2L))).thenReturn(true);
        when(memberFollowRepository.existsById(new MemberFollowId(2L, 1L))).thenReturn(true);

        // when
        MemberResDTO.OtherProfile result = memberQueryService.getOtherProfile(1L, 2L);

        // then
        assertThat(result.isFollowing()).isTrue();
        assertThat(result.isFollowingViewer()).isTrue();
    }

    @Test
    void isFollowing은_팔로우_관계가_존재하면_true를_반환한다() {
        // given
        when(memberFollowRepository.existsById(new MemberFollowId(1L, 2L))).thenReturn(true);

        // when
        boolean result = memberQueryService.isFollowing(1L, 2L);

        // then
        assertThat(result).isTrue();
    }

    @Test
    void isFollowing은_팔로우_관계가_없으면_false를_반환한다() {
        // given
        when(memberFollowRepository.existsById(new MemberFollowId(1L, 2L))).thenReturn(false);

        // when
        boolean result = memberQueryService.isFollowing(1L, 2L);

        // then
        assertThat(result).isFalse();
    }

    @Test
    void 존재하지_않거나_탈퇴한_회원의_프로필은_조회할_수_없다() {
        // given
        when(memberQueryRepository.findVisibleActiveMember(2L, 1L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberQueryService.getOtherProfile(1L, 2L))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Test
    void 본인의_memberId로_다른_사용자_프로필_조회_API를_호출하면_예외가_발생한다() {
        // when & then
        assertThatThrownBy(() -> memberQueryService.getOtherProfile(1L, 1L))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.CANNOT_VIEW_SELF_PROFILE));

        verify(memberRepository, never())
                .findByIdAndStatusAndDeletedAtIsNull(any(), any());
    }

    @Test
    void 팔로워_목록을_조회한다() {
        // given
        Member member = Member.builder().nickname("예림").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberQueryRepository.findVisibleActiveMember(1L, 99L))
                .thenReturn(Optional.of(member));

        Instant followedAt = Instant.now();
        MemberFollowRow row = new MemberFollowRow(2L, "팔로워", "이름", "key", followedAt, true, true);
        Pagination<MemberFollowRow> page =
                Pagination.<MemberFollowRow>builder()
                        .data(List.of(row))
                        .nextCursor(null)
                        .hasNext(false)
                        .pageSize(10)
                        .build();
        when(memberQueryRepository.findFollowersByMemberId(99L, 1L, null, 10)).thenReturn(page);
        when(profileImageStorage.getPublicUrlOrNull("key")).thenReturn("https://example.com/key");

        // when
        Pagination<MemberResDTO.FollowerItem> result = memberQueryService.findFollowers(99L, 1L, null, 10);

        // then
        assertThat(result.data()).containsExactly(
                new MemberResDTO.FollowerItem(2L, "팔로워", "이름", "https://example.com/key", followedAt, true, true));
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 존재하지_않는_회원의_팔로워_목록은_조회할_수_없다() {
        // given
        when(memberQueryRepository.findVisibleActiveMember(1L, 99L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberQueryService.findFollowers(99L, 1L, null, 10))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));

        verify(memberQueryRepository, never()).findFollowersByMemberId(any(), any(), any(), any());
    }

    @Test
    void 팔로잉_목록을_조회한다() {
        // given
        Member member = Member.builder().nickname("예림").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberQueryRepository.findVisibleActiveMember(1L, 99L))
                .thenReturn(Optional.of(member));

        Instant followedAt = Instant.now();
        MemberFollowRow row = new MemberFollowRow(2L, "팔로잉", "이름", "key", followedAt, true, true);
        Pagination<MemberFollowRow> page =
                Pagination.<MemberFollowRow>builder()
                        .data(List.of(row))
                        .nextCursor(null)
                        .hasNext(false)
                        .pageSize(10)
                        .build();
        when(memberQueryRepository.findFollowingByMemberId(99L, 1L, null, 10)).thenReturn(page);
        when(profileImageStorage.getPublicUrlOrNull("key")).thenReturn("https://example.com/key");

        // when
        Pagination<MemberResDTO.FollowingItem> result = memberQueryService.findFollowing(99L, 1L, null, 10);

        // then
        assertThat(result.data()).containsExactly(
                new MemberResDTO.FollowingItem(2L, "팔로잉", "이름", "https://example.com/key", followedAt, true, true));
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    void 존재하지_않는_회원의_팔로잉_목록은_조회할_수_없다() {
        // given
        when(memberQueryRepository.findVisibleActiveMember(1L, 99L))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberQueryService.findFollowing(99L, 1L, null, 10))
                .isInstanceOfSatisfying(MemberException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND));

        verify(memberQueryRepository, never()).findFollowingByMemberId(any(), any(), any(), any());
    }

    @Test
    void 회원을_검색하면_레포지토리_결과를_DTO로_변환한다() {
        // given
        Instant createdAt = Instant.now();
        MemberSearchRow row = new MemberSearchRow(2L, "닉네임", "이름", "key", createdAt, true, false, 2, 0);
        Pagination<MemberSearchRow> page =
                Pagination.<MemberSearchRow>builder()
                        .data(List.of(row))
                        .nextCursor("next-cursor")
                        .hasNext(true)
                        .pageSize(10)
                        .build();
        when(memberQueryRepository.searchActiveMembers(99L, "키워드", null, 10)).thenReturn(page);
        when(profileImageStorage.getPublicUrlOrNull("key")).thenReturn("https://example.com/key");

        // when
        Pagination<MemberResDTO.SearchItem> result = memberQueryService.searchActiveMembers(99L, "키워드", null, 10);

        // then
        assertThat(result.data()).containsExactly(
                new MemberResDTO.SearchItem(2L, "닉네임", "이름", "https://example.com/key", true, false, createdAt));
        assertThat(result.nextCursor()).isEqualTo("next-cursor");
        assertThat(result.hasNext()).isTrue();
    }

    @Test
    void 검색_결과가_없으면_빈_페이지를_반환한다() {
        // given
        Pagination<MemberSearchRow> emptyPage =
                Pagination.<MemberSearchRow>builder()
                        .data(List.of())
                        .nextCursor(null)
                        .hasNext(false)
                        .pageSize(10)
                        .build();
        when(memberQueryRepository.searchActiveMembers(99L, "키워드", null, 10)).thenReturn(emptyPage);

        // when
        Pagination<MemberResDTO.SearchItem> result = memberQueryService.searchActiveMembers(99L, "키워드", null, 10);

        // then
        assertThat(result.data()).isEmpty();
        assertThat(result.hasNext()).isFalse();
    }
}
