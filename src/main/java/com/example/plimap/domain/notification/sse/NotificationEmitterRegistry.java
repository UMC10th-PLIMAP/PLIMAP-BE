package com.example.plimap.domain.notification.sse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 알림 실시간 푸시를 위해 회원별로 연결된 SSE Emitter를 메모리에 보관한다.
 * 단일 인스턴스 배포를 전제로 하며, 여러 인스턴스로 확장 시 별도의 pub/sub(Redis 등)이 필요하다.
 */
@Slf4j
@Component
public class NotificationEmitterRegistry {

    private final Map<Long, List<SseEmitter>> emittersByMemberId = new ConcurrentHashMap<>();

    public void save(Long memberId, SseEmitter emitter) {
        emittersByMemberId.computeIfAbsent(memberId, id -> new CopyOnWriteArrayList<>()).add(emitter);
    }

    public void delete(Long memberId, SseEmitter emitter) {
        emittersByMemberId.computeIfPresent(memberId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    public void sendToMember(Long memberId, String eventName, Object data) {
        List<SseEmitter> emitters = emittersByMemberId.get(memberId);
        if (emitters == null) {
            return;
        }

        for (SseEmitter emitter : List.copyOf(emitters)) {
            send(memberId, emitter, eventName, data);
        }
    }

    public void sendHeartbeatToAll() {
        emittersByMemberId.forEach((memberId, emitters) -> {
            for (SseEmitter emitter : List.copyOf(emitters)) {
                send(memberId, emitter, "heartbeat", "ping");
            }
        });
    }

    private void send(Long memberId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            log.debug("SSE 전송 실패로 emitter를 제거합니다. memberId={}", memberId, e);
            delete(memberId, emitter);
        }
    }
}
