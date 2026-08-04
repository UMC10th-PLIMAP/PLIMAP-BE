package com.example.plimap.domain.member.service.query.impl;

import com.example.plimap.domain.member.converter.MemberConverter;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.entity.MemberFollowId;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.NicknameCheckFailReason;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberFollowRepository;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.repository.query.MemberQueryRepository;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import com.vane.badwordfiltering.BadWordFiltering;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryService {

    private static final int NICKNAME_MIN_LENGTH = 2;
    private static final int NICKNAME_MAX_LENGTH = 10;
    private static final Pattern NICKNAME_FORMAT = Pattern.compile("^[가-힣A-Za-z0-9]+$");
    // BadWordFiltering.check()는 대소문자를 구분하는 완전 일치 substring 검사라 브랜드 사칭 방지용 단어는 별도로 대소문자 무시 검사한다.
    private static final List<String> CUSTOM_FORBIDDEN_WORDS = List.of("plimap", "플리맵운영자", "플리맵사용자");

    private final MemberRepository memberRepository;
    private final MemberFollowRepository memberFollowRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final ProfileImageStorage profileImageStorage;
    private final BadWordFiltering badWordFiltering = new BadWordFiltering();

    @Override
    public Member getActiveMember(Long memberId) {
        return memberRepository.findByIdAndStatusAndDeletedAtIsNull(memberId, MemberStatus.ACTIVE)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    @Override
    public List<Member> findAllFollowers(Long memberId) {
        return memberFollowRepository.findAllByIdFollowingId(memberId, MemberStatus.ACTIVE).stream()
                .map(MemberFollow::getFollower)
                .toList();
    }

    @Override
    public boolean isNicknameAvailable(String nickname) {
        return !memberRepository.existsByNicknameIgnoreCaseAndDeletedAtIsNull(nickname);
    }

    @Override
    public boolean isNicknameForbidden(String nickname) {
        String lowerNickname = nickname.toLowerCase(Locale.ROOT);
        return badWordFiltering.check(nickname) || CUSTOM_FORBIDDEN_WORDS.stream().anyMatch(lowerNickname::contains);
    }

    @Override
    public NicknameCheckFailReason checkNicknameFailReason(String nickname) {
        if (nickname.length() < NICKNAME_MIN_LENGTH) {
            return NicknameCheckFailReason.TOO_SHORT;
        }
        if (nickname.length() > NICKNAME_MAX_LENGTH) {
            return NicknameCheckFailReason.TOO_LONG;
        }
        if (!NICKNAME_FORMAT.matcher(nickname).matches()) {
            return NicknameCheckFailReason.INVALID_FORMAT;
        }
        if (isNicknameForbidden(nickname)) {
            return NicknameCheckFailReason.FORBIDDEN_WORD;
        }
        if (!isNicknameAvailable(nickname)) {
            return NicknameCheckFailReason.DUPLICATE;
        }
        return null;
    }

    @Override
    public MemberResDTO.MyProfile getMyProfile(Long memberId) {
        Member member = getActiveMember(memberId);
        long followerCount = memberFollowRepository.countByIdFollowingId(memberId);
        long followingCount = memberFollowRepository.countByIdFollowerId(memberId);
        String profileImageUrl = profileImageStorage.getPublicUrlOrNull(member.getProfileImageObjectKey());
        return MemberConverter.toMyProfile(member, profileImageUrl, followerCount, followingCount);
    }

    @Override
    public MemberResDTO.OtherProfile getOtherProfile(Long viewerId, Long targetMemberId) {
        if (viewerId.equals(targetMemberId)) {
            throw new MemberException(MemberErrorCode.CANNOT_VIEW_SELF_PROFILE);
        }

        Member member = getVisibleActiveMember(targetMemberId, viewerId);
        long followerCount = memberFollowRepository.countByIdFollowingId(targetMemberId);
        long followingCount = memberFollowRepository.countByIdFollowerId(targetMemberId);
        boolean isFollowing = memberFollowRepository.existsById(new MemberFollowId(viewerId, targetMemberId));
        String profileImageUrl = profileImageStorage.getPublicUrlOrNull(member.getProfileImageObjectKey());
        return MemberConverter.toOtherProfile(member, profileImageUrl, followerCount, followingCount, isFollowing);
    }

    @Override
    public Pagination<MemberResDTO.FollowerItem> findFollowers(Long viewerId, Long memberId, String cursor, Integer pageSize) {
        getVisibleActiveMember(memberId, viewerId);
        return memberQueryRepository.findFollowersByMemberId(viewerId, memberId, cursor, pageSize);
    }

    @Override
    public Pagination<MemberResDTO.FollowingItem> findFollowing(Long viewerId, Long memberId, String cursor, Integer pageSize) {
        getVisibleActiveMember(memberId, viewerId);
        return memberQueryRepository.findFollowingByMemberId(viewerId, memberId, cursor, pageSize);
    }

    private Member getVisibleActiveMember(Long targetMemberId, Long viewerId) {
        return memberQueryRepository.findVisibleActiveMember(targetMemberId, viewerId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
