package com.example.plimap.domain.place.repository.lock.impl;

import com.example.plimap.domain.place.repository.lock.PlaceLockRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class PlaceLockRepositoryImpl implements PlaceLockRepository {

    private static final long MAP_SELECTION_LOCK_KEY = 0x504C494D41504D53L;

    private final EntityManager entityManager;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void acquireMapSelectionLock() {
        entityManager.createNativeQuery("SELECT 1 FROM pg_advisory_xact_lock(:lockKey)")
                .setParameter("lockKey", MAP_SELECTION_LOCK_KEY)
                .getSingleResult();
    }
}
