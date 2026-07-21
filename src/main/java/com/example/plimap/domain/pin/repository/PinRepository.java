package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinRepository extends JpaRepository<Pin, Long> {
    Boolean existsByMemberAndPlaceAndDeletedAtIsNull(Member member, Place place);
}
