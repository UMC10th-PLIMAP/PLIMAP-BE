package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.entity.PlaceTrack;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceTrackRepository extends JpaRepository<PlaceTrack, Long> {

    @Query("""
            SELECT placeTrack
            FROM PlaceTrack placeTrack
            JOIN FETCH placeTrack.track
            WHERE placeTrack.id = :placeTrackId
              AND placeTrack.deletedAt IS NULL
            """)
    Optional<PlaceTrack> findDetailByIdAndDeletedAtIsNull(
            @Param("placeTrackId") Long placeTrackId
    );

    Optional<PlaceTrack> findByPlace_IdAndTrack_IdAndDeletedAtIsNull(Long placeId, Long trackId);

    Optional<PlaceTrack> findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(
            Long placeId,
            Long trackId
    );
}
