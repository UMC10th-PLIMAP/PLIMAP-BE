package com.example.plimap.domain.member.repository.query;

import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MemberQueryRepository {

    Pagination<MemberFollowRow> findFollowersByMemberId(Long viewerId, Long memberId, String cursor, Integer pageSize);

    Pagination<MemberFollowRow> findFollowingByMemberId(Long viewerId, Long memberId, String cursor, Integer pageSize);

    Optional<Member> findVisibleActiveMember(Long targetMemberId, Long viewerId);

    Page<Member> searchMembers(String query, MemberStatus status, Pageable pageable);

    Pagination<MemberSearchRow> searchActiveMembers(Long viewerId, String keyword, String cursor, Integer pageSize);
}
