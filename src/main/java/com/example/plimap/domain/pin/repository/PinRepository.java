package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.entity.PlaceTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PinRepository extends JpaRepository<Pin, Long> {
    Boolean existsByMemberAndPlaceAndDeletedAtIsNull(Member member, Place place);
    Optional<Pin> findByIdAndDeletedAtIsNull(Long id);

    @Query("select p.id from Pin p where p.member.id = :memberId")
    List<Long> findIdsByMemberId(Long memberId);

    @Modifying
    @Query("delete from Pin p where p.member.id = :memberId")
    void deleteByMemberId(Long memberId);

    @Modifying
    @Query("""
        update Pin p
        set p.likeCount = p.likeCount + 1
        where p.id = :pinId
    """)
    void increaseLikeCount(Long pinId);

    @Modifying
    @Query("""
        update Pin p
        set p.likeCount = p.likeCount - 1
        where p.id = :pinId
    """)
    void decreaseLikeCount(Long pinId);

    @Modifying
    @Query("""
        update Pin p
        set p.reportCount = p.reportCount + 1
        where p.id = :pinId
    """)
    void increaseReportCount(Long pinId);

    // reportCount > 0 조건으로 감소 후에도 chk_pin_report_count(>= 0) 제약을 항상 만족시킨다.
    // 관리자가 이미 reportCount를 0으로 리셋한 뒤 그 신고 row가 뒤늦게 삭제되는 경우에도 안전하다.
    @Modifying
    @Query("""
        update Pin p
        set p.reportCount = p.reportCount - 1
        where p.id = :pinId and p.reportCount > 0
    """)
    void decreaseReportCount(Long pinId);

    @Query("""
        SELECT p.likeCount
        FROM Pin p
        WHERE p.id = :pinId
          AND p.deletedAt IS NULL
    """)
    Integer findLikeCountById(Long pinId);
}
