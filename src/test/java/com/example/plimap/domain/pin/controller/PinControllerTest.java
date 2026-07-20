package com.example.plimap.domain.pin.controller;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.service.command.impl.PinCommandServiceImpl;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    void 핀_등록에_성공하면_200을_반환한다() throws Exception {
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
                .andExpect(status().isOk())
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

}