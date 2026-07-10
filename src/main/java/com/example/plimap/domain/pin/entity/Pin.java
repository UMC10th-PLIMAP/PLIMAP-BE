package com.example.plimap.domain.pin.entity;

import com.example.plimap.global.entity.BaseEntity;
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
public class Pin extends BaseEntity {

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

    // member 연관관계

    // place_track 연관관계

    @OneToMany(mappedBy = "pin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PinTag> pinTagList = new ArrayList<>();

//    @Builder
//    private Pin(Member member, Place place, Track track, String introduction, Integer clipStartMs, boolean isFeedPublic) {
//        this.member = member;
//        this.place = place;
//        this.track = track;
//        this.introduction = introduction;
//        this.clipStartMs = clipStartMs;
//        this.isFeedPublic = isFeedPublic;
//    }
//
//    public static Pin create(Member member, Place place, Track track, String introduction, Integer clipStartMs, boolean isFeedPublic) {
//        return Pin.builder()
//                .member(member)
//                .place(place)
//                .track(track)
//                .introduction(introduction)
//                .clipStartMs(clipStartMs)
//                .isFeedPublic(isFeedPublic)
//                .build();
//    }
}
