package com.example.plimap.domain.track.repository;

import com.example.plimap.domain.track.entity.PlaceTrackLike;
import com.example.plimap.domain.track.entity.PlaceTrackLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceTrackLikeRepository
        extends JpaRepository<PlaceTrackLike, PlaceTrackLikeId> {
}
