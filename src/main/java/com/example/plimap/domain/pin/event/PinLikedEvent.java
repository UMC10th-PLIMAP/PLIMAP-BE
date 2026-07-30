package com.example.plimap.domain.pin.event;

public record PinLikedEvent(Long pinId, Long pinOwnerId, Long likerId) {}
