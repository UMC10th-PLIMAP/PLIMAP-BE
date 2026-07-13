package com.example.plimap.domain.pin.repository;

import com.example.plimap.domain.pin.entity.Pin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PinRepository extends JpaRepository<Pin, Long> {
}
