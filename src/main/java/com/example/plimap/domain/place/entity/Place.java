package com.example.plimap.domain.place.entity;

import com.example.plimap.global.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

@Getter
@Entity
@Table(name = "place")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "road_address", length = 255)
    private String roadAddress;

    @Column(name = "place_provider", length = 30)
    private String placeProvider;

    @Column(name = "provider_place_id", length = 255)
    private String providerPlaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private PlaceSource source;

    @Column(name = "location", nullable = false, columnDefinition = "geography(Point,4326)")
    private Point location;

    @Builder
    private Place(
            String name,
            String category,
            String address,
            String roadAddress,
            String placeProvider,
            String providerPlaceId,
            PlaceSource source,
            Point location) {
        this.name = name;
        this.category = category;
        this.address = address;
        this.roadAddress = roadAddress;
        this.placeProvider = placeProvider;
        this.providerPlaceId = providerPlaceId;
        this.source = source;
        this.location = location;
    }

    public static Place createMapSelection(
            String name,
            String address,
            String roadAddress,
            Point location) {
        return Place.builder()
                .name(name)
                .address(address)
                .roadAddress(roadAddress)
                .source(PlaceSource.MAP_SELECTION)
                .location(location)
                .build();
    }

}
