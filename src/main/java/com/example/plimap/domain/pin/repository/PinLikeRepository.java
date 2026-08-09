package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.PinLike;
import com.example.plimap.domain.pin.entity.PinLikeId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PinLikeRepository extends JpaRepository<PinLike, PinLikeId> {
    Optional<PinLike> findByPinAndMember(Pin pin, Member member);

    @Query("select pl.pin.id from PinLike pl where pl.member.id = :memberId")
    List<Long> findPinIdsByMemberId(Long memberId);

    @Modifying
    @Query("delete from PinLike pl where pl.member.id = :memberId")
    void deleteByMemberId(Long memberId);
}
