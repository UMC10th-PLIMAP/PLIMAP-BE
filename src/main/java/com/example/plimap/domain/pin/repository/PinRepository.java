package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.place.entity.Place;
import jakarta.persistence.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PinRepository extends JpaRepository<Pin, Long> {
    Boolean existsByMemberAndPlaceAndDeletedAtIsNull(Member member, Place place);
    Optional<Pin> findByIdAndDeletedAtIsNull(Long id);
}
