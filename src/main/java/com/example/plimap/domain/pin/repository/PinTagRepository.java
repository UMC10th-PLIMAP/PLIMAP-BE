package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.pin.entity.PinTag;
import com.example.plimap.domain.pin.entity.PinTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinTagRepository extends JpaRepository<PinTag, PinTagId> {
}
