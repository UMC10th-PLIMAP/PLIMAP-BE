package com.example.plimap.domain.notification.repository;

import com.example.plimap.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Modifying
    @Query("delete from Notification n where n.recipient.id = :memberId or n.actor.id = :memberId")
    void deleteByMemberId(Long memberId);
}
