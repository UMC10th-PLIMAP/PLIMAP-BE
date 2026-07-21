package com.example.plimap.domain.pin.entity;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.global.entity.SoftDeleteEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "pin")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pin extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clip_start_ms", nullable = false)
    private Integer clipStartMs;

    @Column(name = "introduction", length = 100, nullable = false)
    private String introduction;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(name = "is_feed_public", nullable = false)
    private boolean isFeedPublic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_track_id")
    private PlaceTrack placeTrack;

    @OneToMany(mappedBy = "pin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PinTag> pinTagList = new ArrayList<>();

    @Builder
    private Pin(Member member, Place place, PlaceTrack placeTrack, String introduction, Integer clipStartMs, boolean isFeedPublic) {
        this.member = member;
        this.place = place;
        this.placeTrack = placeTrack;
        this.introduction = introduction;
        this.clipStartMs = clipStartMs;
        this.isFeedPublic = isFeedPublic;
    }

    public static Pin create(
            Member member,
            Place place,
            PlaceTrack placeTrack,
            PinRequest.Create request
    ) {
        return Pin.builder()
                .member(member)
                .place(place)
                .placeTrack(placeTrack)
                .introduction(request.introduction())
                .clipStartMs(request.clipStartMs())
                .isFeedPublic(request.feedOpen())
                .build();
    }
}
