package com.example.plimap.domain.notification.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.notification.controller.docs.NotificationControllerDocs;
import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResDTO;
import com.example.plimap.domain.notification.exception.NotificationSuccessCode;
import com.example.plimap.domain.notification.service.query.NotificationQueryService;
import com.example.plimap.domain.notification.sse.NotificationEmitterRegistry;
import com.example.plimap.global.apiPayload.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController implements NotificationControllerDocs {

    private static final long SSE_TIMEOUT_MILLIS = 30L * 60 * 1000;

    private final NotificationQueryService notificationQueryService;
    private final NotificationEmitterRegistry notificationEmitterRegistry;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<Pagination<NotificationResDTO.Item>>> getNotifications(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        Pagination<NotificationResDTO.Item> response =
                notificationQueryService.findNotifications(currentMember.getMember().getId(), cursor, pageSize);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(NotificationSuccessCode.NOTIFICATIONS_RETRIEVED, response));
    }

    @Override
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AuthMember currentMember) {
        Long memberId = currentMember.getMember().getId();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        notificationEmitterRegistry.save(memberId, emitter);

        emitter.onCompletion(() -> notificationEmitterRegistry.delete(memberId, emitter));
        emitter.onTimeout(() -> notificationEmitterRegistry.delete(memberId, emitter));
        emitter.onError(e -> notificationEmitterRegistry.delete(memberId, emitter));

        try {
            // 연결 직후 최초 1회 전송 — 일부 프록시/브라우저가 헤더만 받고 스트림을 열지 않는 것을 방지
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            log.debug("SSE 연결 확인 전송 실패, memberId={}", memberId, e);
            notificationEmitterRegistry.delete(memberId, emitter);
        }

        return emitter;
    }
}
