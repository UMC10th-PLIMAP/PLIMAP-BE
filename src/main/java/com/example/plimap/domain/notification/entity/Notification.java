package com.example.plimap.domain.notification.entity;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.notification.enums.NotificationType;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "recipient_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_recipient"))
    private Member recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "actor_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_notification_actor"))
    private Member actor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "pin_id",
            foreignKey = @ForeignKey(name = "fk_notification_pin"))
    private Pin pin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private NotificationType type;

    @Column(name = "read_at")
    private Instant readAt;

    @Builder
    private Notification(Member recipient, Member actor, Pin pin, NotificationType type) {
        validateTarget(pin, type);

        this.recipient = recipient;
        this.actor = actor;
        this.pin = pin;
        this.type = type;
    }

    public static Notification create(Member recipient, Member actor, Pin pin, NotificationType type) {
        return Notification.builder()
                .recipient(recipient)
                .actor(actor)
                .pin(pin)
                .type(type)
                .build();
    }

    private static void validateTarget(Pin pin, NotificationType type) {
        boolean requiresPin = type == NotificationType.PIN_CREATED || type == NotificationType.PIN_LIKED;
        if (requiresPin != (pin != null)) {
            throw new IllegalArgumentException("알림 유형과 대상 PIN 존재 여부가 일치하지 않습니다.");
        }
    }

    public boolean isRead() {
        return readAt != null;
    }
}
