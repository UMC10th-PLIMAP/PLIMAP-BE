package com.example.plimap.domain.notification.repository;

import com.example.plimap.domain.notification.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying
    @Query("delete from Notification n where n.pin.id in :pinIds")
    void deleteByPinIdIn(List<Long> pinIds);
}
