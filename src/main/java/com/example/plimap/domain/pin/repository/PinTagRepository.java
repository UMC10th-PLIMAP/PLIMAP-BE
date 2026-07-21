package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.PinTag;
import com.example.plimap.domain.pin.entity.PinTagId;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface PinTagRepository extends JpaRepository<PinTag, PinTagId> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PinTag pt where pt.pin = :pin")
    void deleteByPin(@Param("pin") Pin pin);
}
