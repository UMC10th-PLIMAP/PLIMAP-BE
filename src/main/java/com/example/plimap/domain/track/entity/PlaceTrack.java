package com.example.plimap.domain.track.entity;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.global.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "place_track")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceTrack extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "place_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_place_track_place"))
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "track_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_place_track_track"))
    private Track track;

    @Column(name = "public_pin_count", nullable = false)
    private int publicPinCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Builder
    private PlaceTrack(Place place, Track track) {
        this.place = place;
        this.track = track;
        this.publicPinCount = 0;
        this.likeCount = 0;
    }

    public static PlaceTrack create(Place place, Track track) {
        return PlaceTrack.builder()
                .place(place)
                .track(track)
                .build();
    }

    public void increaseLikeCount() {
        likeCount++;
    }

    public void decreaseLikeCount() {
        if (likeCount > 0) {
            likeCount--;
        }
    }

}
