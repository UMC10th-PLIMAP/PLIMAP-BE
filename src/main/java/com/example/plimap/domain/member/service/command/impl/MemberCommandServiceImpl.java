package com.example.plimap.domain.member.service.command.impl;

import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.entity.MemberFollowId;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberFollowRepository;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberCommandServiceImpl implements MemberCommandService {

    private final MemberRepository memberRepository;
    private final MemberFollowRepository memberFollowRepository;
    private final MemberQueryService memberQueryService;

    @Override
    @Transactional
    public Member completeOnboarding(Long memberId, MemberReqDTO.Onboarding request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        if (member.isOnboarded()) {
            throw new MemberException(MemberErrorCode.ALREADY_ONBOARDED);
        }

        if (!memberQueryService.isNicknameAvailable(request.getNickname())) {
            throw new MemberException(MemberErrorCode.NICKNAME_DUPLICATE);
        }

        try {
            member.completeOnboarding(request.getNickname(), request.getProfileImageObjectKey());
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 닉네임으로 온보딩을 완료하는 경우 사전 체크를 통과했더라도
            // DB의 대소문자 무시 유니크 인덱스(uk_member_nickname_ci)에서 최종적으로 걸러진다.
            throw new MemberException(MemberErrorCode.NICKNAME_DUPLICATE, e);
        }

        return member;
    }

    @Override
    @Transactional
    public Member updateProfile(Long memberId, MemberReqDTO.UpdateProfile request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        boolean nicknameChanged = request.nickname() != null
                && !request.nickname().equalsIgnoreCase(member.getNickname());
        if (nicknameChanged && !memberQueryService.isNicknameAvailable(request.nickname())) {
            throw new MemberException(MemberErrorCode.NICKNAME_DUPLICATE);
        }

        try {
            member.updateProfile(request.nickname(), request.name(), request.introduction(), request.profileImageObjectKey());
            memberRepository.flush();
        } catch (DataIntegrityViolationException e) {
            if (!nicknameChanged) {
                throw e;
            }
            // 동시에 같은 닉네임으로 변경하는 경우 사전 체크를 통과했더라도
            // DB의 대소문자 무시 유니크 인덱스(uk_member_nickname_ci)에서 최종적으로 걸러진다.
            throw new MemberException(MemberErrorCode.NICKNAME_DUPLICATE, e);
        }

        return member;
    }

    @Override
    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new MemberException(MemberErrorCode.CANNOT_FOLLOW_SELF);
        }

        Member follower = memberRepository.findByIdAndDeletedAtIsNull(followerId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
        Member following = memberRepository.findByIdAndDeletedAtIsNull(followingId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        MemberFollowId id = new MemberFollowId(followerId, followingId);
        if (memberFollowRepository.existsById(id)) {
            throw new MemberException(MemberErrorCode.ALREADY_FOLLOWING);
        }

        try {
            memberFollowRepository.saveAndFlush(MemberFollow.create(follower, following));
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 대상을 팔로우하는 경우 사전 체크를 통과했더라도
            // 복합 PK(pk_member_follow)에서 최종적으로 걸러진다.
            throw new MemberException(MemberErrorCode.ALREADY_FOLLOWING, e);
        }
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new MemberException(MemberErrorCode.CANNOT_UNFOLLOW_SELF);
        }

        long deletedCount = memberFollowRepository.deleteByIdFollowerIdAndIdFollowingId(followerId, followingId);
        if (deletedCount == 0) {
            throw new MemberException(MemberErrorCode.NOT_FOLLOWING);
        }
    }
}
