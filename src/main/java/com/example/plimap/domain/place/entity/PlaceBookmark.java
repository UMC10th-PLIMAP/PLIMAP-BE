package com.example.plimap.domain.place.entity;

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
@Table(name = "place_bookmark")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceBookmark extends BaseEntity {

    @EmbeddedId
    private PlaceBookmarkId id;

    @MapsId("placeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "place_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_place_bookmark_place"))
    private Place place;

    @Builder
    private PlaceBookmark(Place place, Long memberId) {
        this.place = place;
        this.id = new PlaceBookmarkId(place.getId(), memberId);
    }

}
