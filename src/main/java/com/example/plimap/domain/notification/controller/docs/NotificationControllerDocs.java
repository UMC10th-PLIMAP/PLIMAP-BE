package com.example.plimap.domain.notification.controller.docs;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.notification.dto.Pagination;
import com.example.plimap.domain.notification.dto.response.NotificationResDTO;
import com.example.plimap.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Tag(name = "Notification", description = "알림 관련 API")
public interface NotificationControllerDocs {

    @Operation(
            summary = "알림 목록 조회",
            description = """
                    로그인한 회원의 알림 목록을 최신순으로 커서 기반 페이지네이션으로 조회합니다.

                    **알림 유형(type)**
                    - FOLLOW: 다른 회원이 나를 팔로우했을 때
                    - PIN_CREATED: 내가 팔로우한 회원이 PIN을 등록했을 때
                    - PIN_LIKED: 다른 회원이 내 PIN에 좋아요를 눌렀을 때
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            content = @Content(examples = @ExampleObject(value = """
                    {
                      "isSuccess": true,
                      "code": "NOTIFICATION_NOTIFICATIONS_RETRIEVED_SUCCESS",
                      "message": "알림 목록을 조회했습니다.",
                      "result": {
                        "data": [
                          {
                            "notificationId": 1,
                            "type": "PIN_LIKED",
                            "actorId": 2,
                            "actorNickname": "플리맵유저",
                            "actorProfileImageUrl": null,
                            "isFollowing": false,
                            "isFollowingViewer": false,
                            "pinId": 10,
                            "placeName": "여의도 한강공원",
                            "albumImageUrl": "https://example.com/album.jpg",
                            "read": false,
                            "createdAt": "2026-07-29T10:00:00Z"
                          }
                        ],
                        "nextCursor": null,
                        "hasNext": false,
                        "pageSize": 10
                      }
                    }
                    """))
    )
    ResponseEntity<ApiResponse<Pagination<NotificationResDTO.Item>>> getNotifications(
            @AuthenticationPrincipal AuthMember currentMember,
            @RequestParam(required = false, defaultValue = "10")
            @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
            @Max(value = 50, message = "페이지 크기는 50 이하여야 합니다.")
            Integer pageSize,
            @RequestParam(required = false) String cursor
    );

    @Operation(
            summary = "알림 실시간 구독 (SSE)",
            description = """
                    Server-Sent Events(SSE) 연결을 열어 팔로우/PIN 등록/PIN 좋아요 알림을 실시간으로 수신합니다.

                    - 연결 직후 `connect` 이벤트가 한 번 전송됩니다.
                    - 이후 알림이 생성될 때마다 `notification` 이벤트로 알림 목록 조회와 동일한 형식의 데이터가 전송됩니다.
                    - 약 20초 간격으로 `heartbeat` 이벤트가 전송되어 연결 유지에 사용됩니다.
                    - 연결은 약 50초마다 서버에서 정상 종료되며, 클라이언트는 `EventSource`의 기본 자동 재연결 동작으로 다시 구독해야 합니다.
                    - 연결이 끊긴 짧은 순간에 생성된 알림은 실시간으로 전달되지 않을 수 있으며, 이 경우 알림 목록 조회 API로 확인할 수 있습니다.
                    - 브라우저의 `EventSource`는 커스텀 헤더를 지원하지 않으므로, 로그인 시 발급되는 `accessToken` 쿠키로 인증합니다.
                    """
    )
    SseEmitter subscribe(@AuthenticationPrincipal AuthMember currentMember);
}
