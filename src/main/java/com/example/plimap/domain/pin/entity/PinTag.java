package com.example.plimap.domain.pin.entity;

import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@IdClass(PinTagId.class)
@Table(name = "pin_tag")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PinTag extends BaseEntity {

    @Column(name = "display_order", nullable = false)
    private Short displayOrder;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    private Pin pin;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Builder
    private PinTag(Pin pin, Tag tag, Short displayOrder) {
        this.pin = pin;
        this.tag = tag;
        this.displayOrder = displayOrder;
    }

    public static PinTag create(Pin pin, Tag tag, Short displayOrder) {
        return PinTag.builder()
                .pin(pin)
                .tag(tag)
                .displayOrder(displayOrder)
                .build();
    }
}
