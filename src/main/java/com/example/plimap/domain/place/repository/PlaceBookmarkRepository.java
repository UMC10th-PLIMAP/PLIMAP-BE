package com.example.plimap.domain.place.repository;

import com.example.plimap.domain.place.entity.PlaceBookmark;
import com.example.plimap.domain.place.entity.PlaceBookmarkId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceBookmarkRepository
        extends JpaRepository<PlaceBookmark, PlaceBookmarkId> {

    @Modifying
    @Query(value = """
            INSERT INTO place_bookmark (place_id, member_id, created_at, updated_at)
            VALUES (:placeId, :memberId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            ON CONFLICT (place_id, member_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("placeId") Long placeId,
            @Param("memberId") Long memberId
    );

    @Modifying
    @Query("""
            DELETE FROM PlaceBookmark bookmark
            WHERE bookmark.id.placeId = :placeId
              AND bookmark.id.memberId = :memberId
            """)
    int deleteByPlaceIdAndMemberId(
            @Param("placeId") Long placeId,
            @Param("memberId") Long memberId
    );
}
