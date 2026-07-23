package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PinRepository extends JpaRepository<Pin, Long> {
    Boolean existsByMemberAndPlaceAndDeletedAtIsNull(Member member, Place place);
    Optional<Pin> findByIdAndDeletedAtIsNull(Long id);

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
}
