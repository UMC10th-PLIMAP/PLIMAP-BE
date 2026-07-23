package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.PinLike;
import com.example.plimap.domain.pin.entity.PinLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PinLikeRepository extends JpaRepository<PinLike, PinLikeId> {
    Optional<PinLike> findByPinAndMember(Pin pin, Member member);
}
