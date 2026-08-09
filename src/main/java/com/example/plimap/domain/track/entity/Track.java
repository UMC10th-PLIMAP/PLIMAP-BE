package com.example.plimap.domain.track.entity;

import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "track",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_track_provider_id",
                        columnNames = {"provider", "provider_track_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "provider_track_id", nullable = false, length = 255)
    private String providerTrackId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "artist_name", nullable = false, length = 200)
    private String artistName;

    @Column(name = "album_title", length = 200)
    private String albumTitle;

    @Column(name = "album_image_url", length = 1000)
    private String albumImageUrl;

    @Column(name = "preview_url", length = 1000)
    private String previewUrl;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Builder
    private Track(
            String provider,
            String providerTrackId,
            String title,
            String artistName,
            String albumTitle,
            String albumImageUrl,
            String previewUrl,
            Integer durationMs) {
        this.provider = provider;
        this.providerTrackId = providerTrackId;
        this.title = title;
        this.artistName = artistName;
        this.albumTitle = albumTitle;
        this.albumImageUrl = albumImageUrl;
        this.previewUrl = previewUrl;
        this.durationMs = durationMs;
    }

    public static Track create(
            String provider,
            String providerTrackId,
            String title,
            String artistName,
            String albumTitle,
            String albumImageUrl,
            String previewUrl,
            Integer durationMs) {
        return Track.builder()
                .provider(provider)
                .providerTrackId(providerTrackId)
                .title(title)
                .artistName(artistName)
                .albumTitle(albumTitle)
                .albumImageUrl(albumImageUrl)
                .previewUrl(previewUrl)
                .durationMs(durationMs)
                .build();
    }

}
