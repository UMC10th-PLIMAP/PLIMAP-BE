package com.example.plimap.domain.pin.entity;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@IdClass(PinLikeId.class)
@Table(name = "pin_like")
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PinLike extends BaseEntity {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pin_id", nullable = false)
    private Pin pin;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    private PinLike(Pin pin, Member member) {
        this.pin = pin;
        this.member = member;
    }

    public static PinLike create(Pin pin, Member member) {
        return PinLike.builder()
                .pin(pin)
                .member(member)
                .build();
    }
}
