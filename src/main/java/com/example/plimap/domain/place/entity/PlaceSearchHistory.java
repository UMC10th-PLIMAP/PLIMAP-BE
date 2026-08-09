package com.example.plimap.domain.place.entity;

import com.example.plimap.global.entity.BaseEntity;
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
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Getter
@Entity
@Table(name = "place_search_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceSearchHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "place_id",
            foreignKey = @ForeignKey(name = "fk_place_search_history_place"))
    private Place place;

    @Column(name = "place_provider", length = 30)
    private String placeProvider;

    @Column(name = "provider_place_id", length = 255)
    private String providerPlaceId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    @Builder
    private PlaceSearchHistory(
            Long memberId,
            Place place,
            String placeProvider,
            String providerPlaceId,
            String placeName,
            String category,
            String address,
            Point location,
            Instant selectedAt) {
        this.memberId = memberId;
        this.place = place;
        this.placeProvider = placeProvider;
        this.providerPlaceId = providerPlaceId;
        this.placeName = placeName;
        this.category = category;
        this.address = address;
        this.location = location;
        this.selectedAt = selectedAt;
    }

}
