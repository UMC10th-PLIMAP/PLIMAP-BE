package com.example.plimap.domain.place.repository;

import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.entity.PlaceSource;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByIdAndDeletedAtIsNull(Long id);

    Optional<Place> findByPlaceProviderAndProviderPlaceIdAndDeletedAtIsNull(
            String placeProvider,
            String providerPlaceId
    );

    Optional<Place> findBySourceAndNormalizedAddressAndDeletedAtIsNull(
            PlaceSource source,
            String normalizedAddress
    );

    List<Place> findAllByPlaceProviderAndProviderPlaceIdInAndDeletedAtIsNull(
            String placeProvider,
            Collection<String> providerPlaceIds
    );
}
