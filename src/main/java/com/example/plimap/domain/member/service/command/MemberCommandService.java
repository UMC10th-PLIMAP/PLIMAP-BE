package com.example.plimap.domain.member.service.command;

import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import org.springframework.web.multipart.MultipartFile;

public interface MemberCommandService {

    Member completeOnboarding(Long memberId, MemberReqDTO.Onboarding request);

    MemberResDTO.Profile updateProfile(Long memberId, MemberReqDTO.UpdateProfile request);

    MemberResDTO.ProfileImage uploadProfileImage(Long memberId, MultipartFile image);

    void follow(Long followerId, Long followingId);

    void unfollow(Long followerId, Long followingId);

    void increaseReportCount(Long memberId);

    void decreaseReportCount(Long memberId);

    void withdraw(Long memberId);

    boolean increasePenaltyPoint(Long memberId);

    void liftSuspension(Long memberId);

    void replacePenalizedNickname(Long memberId, String newNickname);

    void resetReportCount(Long memberId);
}
