package com.example.plimap.domain.place.repository.lock;

public interface PlaceLockRepository {

    void acquireMapSelectionLock();

    void acquirePlaceSelectionLock(String provider, String providerPlaceId);
}
