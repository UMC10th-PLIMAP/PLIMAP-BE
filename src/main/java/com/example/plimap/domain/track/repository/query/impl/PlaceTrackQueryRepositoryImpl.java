package com.example.plimap.domain.track.repository.query.impl;

import static com.example.plimap.domain.pin.entity.QPin.pin;
import static com.example.plimap.domain.track.entity.QPlaceTrack.placeTrack;
import static com.example.plimap.domain.track.entity.QPlaceTrackLike.placeTrackLike;
import static com.example.plimap.domain.track.entity.QTrack.track;

import com.example.plimap.domain.track.dto.PlaceTrackQueryResult;
import com.example.plimap.domain.track.enums.PlaceTrackSort;
import com.example.plimap.domain.track.repository.query.PlaceTrackQueryRepository;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
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

    private static final String PLACE_BOOKMARK_EXISTS_QUERY = """
            SELECT EXISTS (
                SELECT 1
                FROM place_bookmark pb
                WHERE pb.place_id = :placeId
                  AND pb.member_id = :memberId
            )
            """;

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;

    @Override
    public Slice<PlaceTrackQueryResult> findPlaceTracks(
            Long placeId,
            Long memberId,
            PlaceTrackSort sort,
            Pageable pageable
    ) {
        NumberExpression<Long> pinCount = pin.id.count();
        BooleanExpression liked = new CaseBuilder()
                .when(placeTrackLike.id.memberId.eq(memberId))
                .then(1L)
                .otherwise(0L)
                .sumAggregate()
                .gt(0L);

        JPAQuery<Tuple> query = queryFactory
                .select(
                        placeTrack.id,
                        track.title,
                        track.artistName,
                        track.albumImageUrl,
                        pinCount,
                        placeTrack.likeCount,
                        liked
                )
                .from(placeTrack)
                .join(placeTrack.track, track)
                .join(pin)
                .on(
                        pin.placeTrack.eq(placeTrack),
                        pin.place.eq(placeTrack.place),
                        pin.deletedAt.isNull()
                )
                .leftJoin(placeTrackLike)
                .on(
                        placeTrackLike.placeTrack.eq(placeTrack),
                        placeTrackLike.id.memberId.eq(memberId)
                )
                .where(
                        placeTrack.place.id.eq(placeId),
                        placeTrack.deletedAt.isNull()
                )
                .groupBy(
                        placeTrack.id,
                        track.title,
                        track.artistName,
                        track.albumImageUrl,
                        placeTrack.likeCount
                );

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
        if (sort == PlaceTrackSort.POPULAR) {
            orderSpecifiers.add(placeTrack.likeCount.desc());
        }
        orderSpecifiers.add(pin.createdAt.max().desc());
        orderSpecifiers.add(placeTrack.id.desc());

        List<Tuple> rows = query
                .orderBy(orderSpecifiers.toArray(OrderSpecifier<?>[]::new))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1L)
                .fetch();
        boolean hasNext = rows.size() > pageable.getPageSize();
        int contentSize = hasNext ? pageable.getPageSize() : rows.size();
        List<PlaceTrackQueryResult> content = new ArrayList<>(contentSize);

        for (int index = 0; index < contentSize; index++) {
            content.add(toQueryResult(
                    rows.get(index),
                    pinCount,
                    liked
            ));
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

    private PlaceTrackQueryResult toQueryResult(
            Tuple row,
            NumberExpression<Long> pinCount,
            BooleanExpression liked
    ) {
        return new PlaceTrackQueryResult(
                row.get(placeTrack.id),
                row.get(track.title),
                row.get(track.artistName),
                row.get(track.albumImageUrl),
                Math.toIntExact(row.get(pinCount)),
                row.get(placeTrack.likeCount),
                row.get(liked)
        );
    }
}
