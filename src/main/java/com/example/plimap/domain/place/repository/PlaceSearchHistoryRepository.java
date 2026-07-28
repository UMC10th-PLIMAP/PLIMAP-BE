package com.example.plimap.domain.place.repository;

import com.example.plimap.domain.place.entity.PlaceSearchHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaceSearchHistoryRepository extends JpaRepository<PlaceSearchHistory, Long> {

    @EntityGraph(attributePaths = "place")
    List<PlaceSearchHistory> findTop5ByMemberIdOrderBySelectedAtDescIdDesc(Long memberId);

    @Modifying
    @Query("DELETE FROM PlaceSearchHistory history "
            + "WHERE history.id = :historyId AND history.memberId = :memberId")
    int deleteByIdAndMemberId(
            @Param("historyId") Long historyId,
            @Param("memberId") Long memberId
    );

    @Modifying
    @Query(value = """
            INSERT INTO place_search_history (
                member_id,
                place_id,
                place_name,
                category,
                address,
                location,
                selected_at,
                created_at,
                updated_at
            )
            SELECT
                :memberId,
                place.id,
                place.name,
                place.category,
                place.address,
                place.location,
                clock_timestamp(),
                clock_timestamp(),
                clock_timestamp()
            FROM place
            WHERE place.id = :placeId
            ON CONFLICT (member_id, place_id) WHERE place_id IS NOT NULL
            DO UPDATE SET
                selected_at = clock_timestamp(),
                updated_at = clock_timestamp()
            """, nativeQuery = true)
    int upsert(
            @Param("memberId") Long memberId,
            @Param("placeId") Long placeId
    );

    @Modifying
    @Query(value = """
            DELETE FROM place_search_history
            WHERE id IN (
                SELECT id
                FROM place_search_history
                WHERE member_id = :memberId
                ORDER BY selected_at DESC, id DESC
                OFFSET 5
            )
            """, nativeQuery = true)
    int deleteExcessByMemberId(@Param("memberId") Long memberId);
}
