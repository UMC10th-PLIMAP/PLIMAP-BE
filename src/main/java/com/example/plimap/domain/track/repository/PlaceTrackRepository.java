package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.entity.PlaceTrack;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceTrackRepository extends JpaRepository<PlaceTrack, Long> {

    @Query("""
            SELECT placeTrack
            FROM PlaceTrack placeTrack
            JOIN FETCH placeTrack.track
            WHERE placeTrack.id = :placeTrackId
              AND placeTrack.deletedAt IS NULL
              AND placeTrack.place.deletedAt IS NULL
            """)
    Optional<PlaceTrack> findDetailByIdAndDeletedAtIsNull(
            @Param("placeTrackId") Long placeTrackId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT placeTrack
            FROM PlaceTrack placeTrack
            JOIN FETCH placeTrack.place
            WHERE placeTrack.id = :placeTrackId
              AND placeTrack.deletedAt IS NULL
              AND placeTrack.place.deletedAt IS NULL
            """)
    Optional<PlaceTrack> findActiveByIdForUpdate(
            @Param("placeTrackId") Long placeTrackId
    );

    Optional<PlaceTrack> findByPlace_IdAndTrack_IdAndDeletedAtIsNull(Long placeId, Long trackId);

    Optional<PlaceTrack> findFirstByPlace_IdAndTrack_IdAndDeletedAtIsNotNullOrderByIdDesc(
            Long placeId,
            Long trackId
    );

    @Query("""
        select pt
        from PlaceTrack pt
        join fetch pt.place
        where pt.id = :id
          and pt.deletedAt is null
          and pt.place.deletedAt is null
    """)
    Optional<PlaceTrack> findWithPlaceById(Long id);
}
