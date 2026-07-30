package com.example.plimap.domain.notification.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SSE 연결이 프록시/로드밸런서의 idle timeout으로 끊기지 않도록 주기적으로 heartbeat를 보낸다.
 */
@Component
@RequiredArgsConstructor
public class NotificationHeartbeatScheduler {

    private final NotificationEmitterRegistry notificationEmitterRegistry;

    @Scheduled(fixedRate = 20_000)
    public void sendHeartbeat() {
        notificationEmitterRegistry.sendHeartbeatToAll();
    }
}
