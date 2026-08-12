package com.example.plimap.domain.member.enums;

import java.time.Duration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SuspensionPeriod {

    ONE_DAY(Duration.ofDays(1)),
    THREE_DAYS(Duration.ofDays(3)),
    FIVE_DAYS(Duration.ofDays(5)),
    PERMANENT(null);

    private final Duration duration;

    public boolean isPermanent() {
        return this == PERMANENT;
    }
}
