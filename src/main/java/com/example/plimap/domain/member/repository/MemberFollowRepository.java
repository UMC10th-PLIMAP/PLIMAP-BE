package com.example.plimap.domain.member.repository;

import com.example.plimap.domain.member.entity.MemberFollow;
import com.example.plimap.domain.member.entity.MemberFollowId;
import com.example.plimap.domain.member.enums.MemberStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberFollowRepository extends JpaRepository<MemberFollow, MemberFollowId> {

    @Query("""
            SELECT mf FROM MemberFollow mf JOIN FETCH mf.follower
            WHERE mf.id.followingId = :followingId
            AND mf.follower.status = :status
            AND mf.follower.deletedAt IS NULL
            """)
    List<MemberFollow> findAllByIdFollowingId(
            @Param("followingId") Long followingId,
            @Param("status") MemberStatus status);

    long deleteByIdFollowerIdAndIdFollowingId(Long followerId, Long followingId);

    long countByIdFollowerId(Long followerId);

    long countByIdFollowingId(Long followingId);
}
