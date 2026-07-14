package com.example.plimap.domain.place.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.place.dto.response.PlaceResponse;
import com.example.plimap.domain.place.entity.PlaceSource;
import com.example.plimap.domain.place.service.command.PlaceCommandService;
import com.example.plimap.global.apiPayload.exception.GlobalExceptionHandler;
import com.example.plimap.global.config.CorsConfig;
import com.example.plimap.global.config.SecurityConfig;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.global.security.SecurityErrorResponseHandler;
import com.example.plimap.global.security.TokenBlacklistService;
import java.util.Optional;
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

@WebMvcTest(controllers = PlaceController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class PlaceControllerTest {

    private static final String ENDPOINT = "/api/v1/places/map-selections";
    private static final String ACCESS_TOKEN = "valid-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlaceCommandService placeCommandService;

    @MockitoBean
    private CustomOAuthService customOAuthService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @BeforeEach
    void setUp() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getMemberId(ACCESS_TOKEN)).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(Member.builder().build()));
    }

    @Test
    void 지도_선택_장소_확정에_성공하면_200을_반환한다() throws Exception {
        when(placeCommandService.confirmMapSelection(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PlaceResponse.MapSelection(
                        12L,
                        "물빛무대 앞 광장",
                        PlaceSource.MAP_SELECTION,
                        37.5283,
                        126.9326
                ));

        mockMvc.perform(post(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("PLACE_MAP_SELECTION_SUCCESS"))
                .andExpect(jsonPath("$.message").value("지도 선택 장소 확정에 성공했습니다."))
                .andExpect(jsonPath("$.result.placeId").value(12))
                .andExpect(jsonPath("$.result.placeName").value("물빛무대 앞 광장"))
                .andExpect(jsonPath("$.result.source").value("MAP_SELECTION"))
                .andExpect(jsonPath("$.result.latitude").value(37.5283))
                .andExpect(jsonPath("$.result.longitude").value(126.9326));
    }

    @Test
    void 좌표가_유효_범위를_벗어나면_공통_400을_반환한다() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 91,
                                  "longitude": 126.9326,
                                  "address": "서울특별시 영등포구 여의도동"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("위치 정보가 올바르지 않습니다."))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(placeCommandService);
    }

    @Test
    void address가_blank이면_공통_400을_반환한다() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "latitude": 37.5283,
                                  "longitude": 126.9326,
                                  "address": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_400_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("위치 정보가 올바르지 않습니다."));

        verifyNoInteractions(placeCommandService);
    }

    @Test
    void 유효하지_않은_Bearer_인증은_공통_401을_반환한다() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_401_UNAUTHORIZED"))
                .andExpect(jsonPath("$.result").isEmpty());

        verifyNoInteractions(placeCommandService);
    }

    private String validRequest() {
        return """
                {
                  "latitude": 37.5283,
                  "longitude": 126.9326,
                  "placeName": "물빛무대 앞 광장",
                  "address": "서울특별시 영등포구 여의도동",
                  "roadAddress": "서울특별시 영등포구 여의동로"
                }
                """;
    }
}
