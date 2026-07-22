package com.example.plimap.domain.pin.controller;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.enums.AvailabilityStatus;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.service.command.impl.PinCommandServiceImpl;
import com.example.plimap.domain.pin.service.query.impl.PinQueryServiceImpl;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
import com.example.plimap.global.security.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.SecurityErrorResponseHandler;
import com.example.plimap.global.security.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PinController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class PinControllerTest {

    private static final String PIN_CREATE_ENDPOINT = "/api/v1/pins";
    private static final String PIN_AVAILABILITY_ENDPOINT = "/api/v1/pins/availability";
    private static final String PIN_UPDATE_ENDPOINT = "/api/v1/pins/1";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    JwtUtil jwtUtil;

    @MockitoBean
    private CustomOAuthService customOAuthService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean
    MemberRepository memberRepository;

    @MockitoBean
    TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private PinCommandServiceImpl pinCommandService;

    @MockitoBean
    private PinQueryServiceImpl pinQueryService;

    @MockitoBean
    private OAuthFailureHandler oAuthFailureHandler;

    @MockitoBean
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @BeforeEach
    void setUp() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(ACCESS_TOKEN)).thenReturn("test-jti");
        when(tokenBlacklistService.isBlacklisted("test-jti")).thenReturn(false);
        when(jwtUtil.getMemberId(ACCESS_TOKEN)).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(Member.builder().build()));
    }

    @Test
    void 핀_등록에_성공하면_201을_반환한다() throws Exception {
        when(pinCommandService.createPin(
                any(Member.class),
                any(PinRequest.Create.class)
        )).thenReturn(PinResponse.Summary.builder()
                .pinId(1L)
                .placeId(1L)
                .writerNickname("이서")
                .writerProfileImage("image_url")
                .introduction("feeling love attack!")
                .clipStartMs(70000)
                .build());

        mockMvc.perform(post(PIN_CREATE_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("PIN_CREATED_SUCCESS"))
                .andExpect(jsonPath("$.message").value("핀이 생성되었습니다."))
                .andExpect(jsonPath("$.result.pinId").value(1))
                .andExpect(jsonPath("$.result.placeId").value(1))
                .andExpect(jsonPath("$.result.writerNickname").value("이서"))
                .andExpect(jsonPath("$.result.writerProfileImage").value("image_url"))
                .andExpect(jsonPath("$.result.introduction").value("feeling love attack!"))
                .andExpect(jsonPath("$.result.clipStartMs").value(70000));
    }

    @Test
    void 현위치가_핀위치_거리가_500m_이상일시_400을_반환한다() throws Exception {
        when(pinCommandService.createPin(
                any(Member.class),
                any(PinRequest.Create.class)
        )).thenThrow(new PinException(PinErrorCode.LOCATION_DISTANCE_INVALID));

        mockMvc.perform(post(PIN_CREATE_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(inValidLocationRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("LOCATION_DISTANCE_INVALID"))
                .andExpect(jsonPath("$.message").value("사용자가 장소 반경이 500m 이상에 있어 PIN을 등록할 수 없습니다."));
    }

    @Test
    void 지도_선택위치_검증_등록가능시_200을_반환한다() throws Exception {
        when(pinQueryService.validatePinAvailability(
                any(PinRequest.PinAvailability.class)
        )).thenReturn(PinResponse.PinAvailability.builder()
                .status(AvailabilityStatus.CREATABLE_NEW_PLACE)
                .registrable(true)
                .distanceFromUserMeters(328.98070823323764)
                .nearestPinDistanceMeters(null)
                .build());

        // CREATABLE_NEW_PLACE
        mockMvc.perform(post(PIN_AVAILABILITY_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPinAvailabilityRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("PIN_AVAILABILITY_CHECK_SUCCESS"))
                .andExpect(jsonPath("$.message").value("PIN 등록 가능 여부 검증에 성공했습니다."))
                .andExpect(jsonPath("$.result.status").value("CREATABLE_NEW_PLACE"))
                .andExpect(jsonPath("$.result.registrable").value(true))
                .andExpect(jsonPath("$.result.distanceFromUserMeters").value(328.98070823323764))
                .andExpect(jsonPath("$.result.nearestPinDistanceMeters").doesNotExist());
    }

    @Test
    void 지도_선택위치_검증_500m_초과시_200을_반환한다() throws Exception {
        when(pinQueryService.validatePinAvailability(
                any(PinRequest.PinAvailability.class)
        )).thenReturn(PinResponse.PinAvailability.builder()
                .status(AvailabilityStatus.OUT_OF_RANGE)
                .registrable(false)
                .distanceFromUserMeters(620.0)
                .nearestPinDistanceMeters(null)
                .build());

        // OUT_OF_RANGE
        mockMvc.perform(post(PIN_AVAILABILITY_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPinAvailabilityRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("PIN_AVAILABILITY_CHECK_SUCCESS"))
                .andExpect(jsonPath("$.message").value("PIN 등록 가능 여부 검증에 성공했습니다."))
                .andExpect(jsonPath("$.result.status").value("OUT_OF_RANGE"))
                .andExpect(jsonPath("$.result.registrable").value(false))
                .andExpect(jsonPath("$.result.distanceFromUserMeters").value(620.0))
                .andExpect(jsonPath("$.result.nearestPinDistanceMeters").doesNotExist());
    }

    @Test
    void 지도_선택위치_검증_20m_이내_핀_존재시_200을_반환한다() throws Exception {
        when(pinQueryService.validatePinAvailability(
                any(PinRequest.PinAvailability.class)
        )).thenReturn(PinResponse.PinAvailability.builder()
                .status(AvailabilityStatus.TOO_CLOSE_TO_PIN)
                .registrable(false)
                .distanceFromUserMeters(76.0836069534716)
                .nearestPinDistanceMeters(83.07525620101859)
                .build());

        // TOO_CLOSE_TO_PIN
        mockMvc.perform(post(PIN_AVAILABILITY_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPinAvailabilityRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("PIN_AVAILABILITY_CHECK_SUCCESS"))
                .andExpect(jsonPath("$.message").value("PIN 등록 가능 여부 검증에 성공했습니다."))
                .andExpect(jsonPath("$.result.status").value("TOO_CLOSE_TO_PIN"))
                .andExpect(jsonPath("$.result.registrable").value(false))
                .andExpect(jsonPath("$.result.distanceFromUserMeters").value(76.0836069534716))
                .andExpect(jsonPath("$.result.nearestPinDistanceMeters").value(83.07525620101859));
    }

    @Test
    void 핀_수정에_성공하면_200을_반환한다() throws Exception {
        when(pinCommandService.updatePin(
                any(Member.class),
                any(PinRequest.Update.class),
                anyLong()
        )).thenReturn(PinResponse.UpdatedPin.builder()
                .introduction("feeling love attack!")
                .tags(List.of("청량", "설렘"))
                .feedOpen(true)
                .build());

        mockMvc.perform(patch(PIN_UPDATE_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("PIN_UPDATE_SUCCESS"))
                .andExpect(jsonPath("$.message").value("PIN이 수정되었습니다."))
                .andExpect(jsonPath("$.result.introduction").value("feeling love attack!"))
                .andExpect(jsonPath("$.result.tags[0]").value("청량"))
                .andExpect(jsonPath("$.result.tags[1]").value("설렘"))
                .andExpect(jsonPath("$.result.feedOpen").value(true));
    }

    @Test
    void 수정할_값이_없을시_400을_반환한다() throws Exception {
        when(pinCommandService.updatePin(
                any(Member.class),
                any(PinRequest.Update.class),
                anyLong()
        )).thenThrow(new PinException(PinErrorCode.PIN_NOT_CHANGED));

        mockMvc.perform(patch(PIN_UPDATE_ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUpdateRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("PIN_NOT_CHANGED"))
                .andExpect(jsonPath("$.message").value("PIN 수정사항이 없습니다."))
                .andExpect(jsonPath("$.result").doesNotExist());
    }


    private String validCreateRequest() {
        return """
                {
                   "userLatitude": 37.5297,
                   "userLongitude": 126.9333,
                   "placeId": 1,
                   "itunesTrackId": 1764485170,
                   "clipStartMs": 70000,
                   "introduction": "feeling love attack!",
                   "tags": [
                      "몽환"
                   ],
                   "feedOpen": true
                 }
                """;
    }

    private String inValidLocationRequest() {
        return """
                {
                   "userLatitude": 37.5370,
                   "userLongitude": 126.9326,
                   "placeId": 1,
                   "itunesTrackId": 1764485170,
                   "clipStartMs": 70000,
                   "introduction": "feeling love attack!",
                   "tags": [
                      "몽환"
                   ],
                   "feedOpen": true
                 }
                """;
    }

    private String validPinAvailabilityRequest() {
        return """
                {
                  "latitude": 37.629000,
                  "longitude": 127.094000,
                  "userLatitude": 37.626144976334544,
                  "userLongitude": 127.09302024107471
                }
                """;
    }

    private String validUpdateRequest() {
        return """
                {
                  "introduction": "feeling love attack!",
                  "tags":["청량", "설렘"],
                  "feedOpen": true
                }
                """;
    }

    private String invalidUpdateRequest() {
        return """
                {
                  "introduction": null,
                  "tags":null,
                  "feedOpen": null
                }
                """;
    }

}