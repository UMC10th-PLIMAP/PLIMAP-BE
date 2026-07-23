package com.example.plimap.domain.track.repository.query.impl;

import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import com.example.plimap.domain.track.repository.query.PlaceTrackQueryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlaceTrackQueryRepositoryImpl implements PlaceTrackQueryRepository {

    private static final String PLACE_TRACK_LIST_QUERY = """
            SELECT pt.id,
                   t.title,
                   t.artist_name,
                   t.album_image_url,
                   COUNT(p.id) AS pin_count,
                   pt.like_count,
                   CASE WHEN COUNT(ptl.member_id) > 0 THEN TRUE ELSE FALSE END AS is_liked
            FROM place_track pt
            JOIN track t
              ON t.id = pt.track_id
            JOIN pin p
              ON p.place_track_id = pt.id
             AND p.place_id = pt.place_id
             AND p.deleted_at IS NULL
             AND p.is_feed_public = TRUE
            LEFT JOIN place_track_like ptl
              ON ptl.place_track_id = pt.id
             AND ptl.member_id = :memberId
            WHERE pt.place_id = :placeId
              AND pt.deleted_at IS NULL
            GROUP BY pt.id,
                     t.title,
                     t.artist_name,
                     t.album_image_url,
                     pt.like_count
            ORDER BY CASE WHEN :sort = 'POPULAR' THEN pt.like_count END DESC,
                     MAX(p.created_at) DESC,
                     pt.id DESC
            """;

    private static final String PLACE_BOOKMARK_EXISTS_QUERY = """
            SELECT EXISTS (
                SELECT 1
                FROM place_bookmark pb
                WHERE pb.place_id = :placeId
                  AND pb.member_id = :memberId
            )
            """;

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Slice<PlaceTrackQueryResult> findPlaceTracks(
            Long placeId,
            Long memberId,
            PlaceTrackSort sort,
            Pageable pageable
    ) {
        Query query = entityManager.createNativeQuery(PLACE_TRACK_LIST_QUERY)
                .setParameter("placeId", placeId)
                .setParameter("memberId", memberId)
                .setParameter("sort", sort.name())
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize() + 1);

        List<Object[]> rows = query.getResultList();
        boolean hasNext = rows.size() > pageable.getPageSize();
        int contentSize = hasNext ? pageable.getPageSize() : rows.size();
        List<PlaceTrackQueryResult> content = new ArrayList<>(contentSize);

        for (int index = 0; index < contentSize; index++) {
            Object[] row = rows.get(index);
            content.add(toQueryResult(row));
        }

        return new SliceImpl<>(content, pageable, hasNext);
    }

    @Override
    public boolean existsPlaceBookmark(Long placeId, Long memberId) {
        return (Boolean) entityManager.createNativeQuery(PLACE_BOOKMARK_EXISTS_QUERY)
                .setParameter("placeId", placeId)
                .setParameter("memberId", memberId)
                .getSingleResult();
    }

    private PlaceTrackQueryResult toQueryResult(Object[] row) {
        return new PlaceTrackQueryResult(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                Math.toIntExact(((Number) row[4]).longValue()),
                ((Number) row[5]).intValue(),
                (Boolean) row[6]
        );
    }
}
