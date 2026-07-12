package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.entity.PlaceTrack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceTrackRepository extends JpaRepository<PlaceTrack, Long> {

    Optional<PlaceTrack> findByPlace_IdAndTrack_IdAndDeletedAtIsNull(Long placeId, Long trackId);

    Optional<PlaceTrack> findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(
            Long placeId,
            Long trackId
    );
}
