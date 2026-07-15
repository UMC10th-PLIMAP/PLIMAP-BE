package com.example.plimap.domain.member.service.command;

import com.example.plimap.domain.member.dto.request.MemberReqDTO;
import com.example.plimap.domain.member.entity.Member;

public interface MemberCommandService {

    Member completeOnboarding(Long memberId, MemberReqDTO.Onboarding request);

    void follow(Long followerId, Long followingId);
}
