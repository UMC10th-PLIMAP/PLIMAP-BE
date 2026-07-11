package com.example.plimap.domain.track.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceTrackLikeId implements Serializable {

    @Column(name = "place_track_id", nullable = false)
    private Long placeTrackId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;
}
