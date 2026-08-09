package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.entity.PlaceTrackLike;
import com.example.plimap.domain.track.entity.PlaceTrackLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PlaceTrackLikeRepository
        extends JpaRepository<PlaceTrackLike, PlaceTrackLikeId> {

    boolean existsByIdMemberIdAndPlaceTrackPlaceIdAndPlaceTrackDeletedAtIsNull(
            Long memberId,
            Long placeId
    );

    @Modifying
    @Query("delete from PlaceTrackLike l where l.id.memberId = :memberId")
    void deleteByIdMemberId(Long memberId);
}
