package com.example.plimap.domain.member.repository.query;

import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;

import java.util.Optional;

public interface MemberQueryRepository {

    Pagination<MemberResDTO.FollowerItem> findFollowersByMemberId(Long viewerId, Long memberId, String cursor, Integer pageSize);

    Pagination<MemberResDTO.FollowingItem> findFollowingByMemberId(Long viewerId, Long memberId, String cursor, Integer pageSize);

    Optional<Member> findVisibleActiveMember(Long targetMemberId, Long viewerId);
}
