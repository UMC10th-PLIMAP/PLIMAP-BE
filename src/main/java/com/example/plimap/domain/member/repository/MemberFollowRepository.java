package com.example.plimap.domain.member.repository;

import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.entity.MemberFollowId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberFollowRepository extends JpaRepository<MemberFollow, MemberFollowId> {

    long deleteByIdFollowerIdAndIdFollowingId(Long followerId, Long followingId);
}
