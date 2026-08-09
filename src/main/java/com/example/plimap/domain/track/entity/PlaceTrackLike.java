package com.example.plimap.domain.track.entity;

import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "place_track_like")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceTrackLike extends BaseEntity {

    @EmbeddedId
    private PlaceTrackLikeId id;

    @MapsId("placeTrackId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "place_track_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_place_track_like_place_track"))
    private PlaceTrack placeTrack;

    @Builder
    private PlaceTrackLike(PlaceTrack placeTrack, Long memberId) {
        if (placeTrack == null || placeTrack.getId() == null) {
            throw new IllegalArgumentException("placeTrack must be persisted before creating a like");
        }
        if (memberId == null) {
            throw new IllegalArgumentException("memberId must not be null");
        }

        this.placeTrack = placeTrack;
        this.id = new PlaceTrackLikeId(placeTrack.getId(), memberId);
    }

    public static PlaceTrackLike create(PlaceTrack placeTrack, Long memberId) {
        return PlaceTrackLike.builder()
                .placeTrack(placeTrack)
                .memberId(memberId)
                .build();
    }

}
