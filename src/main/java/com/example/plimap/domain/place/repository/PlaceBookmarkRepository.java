package com.example.plimap.domain.place.repository;

import com.example.plimap.domain.place.entity.PlaceBookmark;
import com.example.plimap.domain.place.entity.PlaceBookmarkId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceBookmarkRepository
        extends JpaRepository<PlaceBookmark, PlaceBookmarkId> {
}
