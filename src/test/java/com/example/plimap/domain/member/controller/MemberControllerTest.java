package com.example.plimap.domain.member.controller;

import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.dto.Pagination;
import com.example.plimap.domain.member.dto.response.MemberResDTO;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.exception.MemberErrorCode;
import com.example.plimap.domain.member.exception.MemberException;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import java.time.Instant;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MemberController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class MemberControllerTest {

    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final Long AUTH_MEMBER_ID = 1L;
    private static final Long TARGET_MEMBER_ID = 2L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private CustomOAuthService customOAuthService;

    @MockitoBean
    private OAuthSuccessHandler oAuthSuccessHandler;

    @MockitoBean
    private OAuthFailureHandler oAuthFailureHandler;

    @MockitoBean
    private MemberRepository memberRepository;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @MockitoBean
    private MemberCommandService memberCommandService;

    @MockitoBean
    private MemberQueryService memberQueryService;

    @BeforeEach
    void setUp() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(ACCESS_TOKEN)).thenReturn("test-jti");
        when(tokenBlacklistService.isBlacklisted("test-jti")).thenReturn(false);
        when(jwtUtil.getMemberId(ACCESS_TOKEN)).thenReturn(AUTH_MEMBER_ID);

        Member authenticatedMember = Member.builder().build();
        ReflectionTestUtils.setField(authenticatedMember, "id", AUTH_MEMBER_ID);
        when(memberRepository.findById(AUTH_MEMBER_ID)).thenReturn(Optional.of(authenticatedMember));
    }

    @Test
    void 내_프로필_조회에_성공하면_200과_MY_PROFILE_FETCHED_응답을_반환한다() throws Exception {
        MemberResDTO.MyProfile profile = new MemberResDTO.MyProfile(
                AUTH_MEMBER_ID, "예림", "이예림", "소개", "key", 3L, 5L, Instant.parse("2026-01-01T00:00:00Z"));
        when(memberQueryService.getMyProfile(AUTH_MEMBER_ID)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/members/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200_MY_PROFILE_FETCHED"))
                .andExpect(jsonPath("$.result.nickname").value("예림"))
                .andExpect(jsonPath("$.result.followerCount").value(3))
                .andExpect(jsonPath("$.result.followingCount").value(5));
    }

    @Test
    void 다른_사용자_프로필_조회에_성공하면_200과_OTHER_PROFILE_FETCHED_응답을_반환한다() throws Exception {
        MemberResDTO.OtherProfile profile = new MemberResDTO.OtherProfile(
                TARGET_MEMBER_ID, "상대방", "김상대", "소개", "key", 3L, 5L, true);
        when(memberQueryService.getOtherProfile(AUTH_MEMBER_ID, TARGET_MEMBER_ID)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/members/{memberId}", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200_OTHER_PROFILE_FETCHED"))
                .andExpect(jsonPath("$.result.nickname").value("상대방"))
                .andExpect(jsonPath("$.result.isFollowing").value(true));
    }

    @Test
    void 존재하지_않는_회원의_프로필을_조회하면_404를_반환한다() throws Exception {
        when(memberQueryService.getOtherProfile(AUTH_MEMBER_ID, TARGET_MEMBER_ID))
                .thenThrow(new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/members/{memberId}", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_404_MEMBER_NOT_FOUND"));
    }

    @Test
    void 본인의_memberId로_다른_사용자_프로필_조회를_요청하면_400을_반환한다() throws Exception {
        when(memberQueryService.getOtherProfile(AUTH_MEMBER_ID, AUTH_MEMBER_ID))
                .thenThrow(new MemberException(MemberErrorCode.CANNOT_VIEW_SELF_PROFILE));

        mockMvc.perform(get("/api/v1/members/{memberId}", AUTH_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_400_CANNOT_VIEW_SELF_PROFILE"));
    }

    @Test
    void 언팔로우에_성공하면_200과_UNFOLLOWED_응답을_반환한다() throws Exception {
        doNothing().when(memberCommandService).unfollow(AUTH_MEMBER_ID, TARGET_MEMBER_ID);

        mockMvc.perform(delete("/api/v1/members/{memberId}/follow", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200_UNFOLLOWED"))
                .andExpect(jsonPath("$.message").value("언팔로우했습니다."))
                .andExpect(jsonPath("$.result").doesNotExist());

        verify(memberCommandService).unfollow(AUTH_MEMBER_ID, TARGET_MEMBER_ID);
    }

    @Test
    void 팔로우_중이_아닌_회원을_언팔로우하면_404를_반환한다() throws Exception {
        doThrow(new MemberException(MemberErrorCode.NOT_FOLLOWING))
                .when(memberCommandService).unfollow(AUTH_MEMBER_ID, TARGET_MEMBER_ID);

        mockMvc.perform(delete("/api/v1/members/{memberId}/follow", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_404_NOT_FOLLOWING"))
                .andExpect(jsonPath("$.message").value("팔로우 중이 아닌 사용자입니다."))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void 팔로워_목록_조회에_성공하면_200과_FOLLOWERS_FETCHED_응답을_반환한다() throws Exception {
        MemberResDTO.FollowerItem follower = new MemberResDTO.FollowerItem(
                3L, "팔로워", "이름", "key", Instant.parse("2026-01-01T00:00:00Z"), true);
        Pagination<MemberResDTO.FollowerItem> page = Pagination.<MemberResDTO.FollowerItem>builder()
                .data(List.of(follower))
                .nextCursor(null)
                .hasNext(false)
                .pageSize(10)
                .build();
        when(memberQueryService.findFollowers(eq(AUTH_MEMBER_ID), eq(TARGET_MEMBER_ID), isNull(), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/v1/members/{memberId}/followers", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200_FOLLOWERS_FETCHED"))
                .andExpect(jsonPath("$.result.data[0].nickname").value("팔로워"))
                .andExpect(jsonPath("$.result.data[0].isFollowing").value(true))
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    void 존재하지_않는_회원의_팔로워_목록을_조회하면_404를_반환한다() throws Exception {
        when(memberQueryService.findFollowers(eq(AUTH_MEMBER_ID), eq(TARGET_MEMBER_ID), isNull(), eq(10)))
                .thenThrow(new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/members/{memberId}/followers", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_404_MEMBER_NOT_FOUND"));
    }

    @Test
    void 팔로워_목록_조회시_pageSize가_50을_초과하면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/members/{memberId}/followers", TARGET_MEMBER_ID)
                        .param("pageSize", "51")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 팔로워_목록_조회시_pageSize가_0이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/members/{memberId}/followers", TARGET_MEMBER_ID)
                        .param("pageSize", "0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 팔로워_목록_조회시_pageSize가_음수면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/members/{memberId}/followers", TARGET_MEMBER_ID)
                        .param("pageSize", "-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 팔로워_목록_조회시_pageSize가_50이면_허용된다() throws Exception {
        Pagination<MemberResDTO.FollowerItem> page = Pagination.<MemberResDTO.FollowerItem>builder()
                .data(List.of())
                .nextCursor(null)
                .hasNext(false)
                .pageSize(50)
                .build();
        when(memberQueryService.findFollowers(eq(AUTH_MEMBER_ID), eq(TARGET_MEMBER_ID), isNull(), eq(50))).thenReturn(page);

        mockMvc.perform(get("/api/v1/members/{memberId}/followers", TARGET_MEMBER_ID)
                        .param("pageSize", "50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk());

        verify(memberQueryService).findFollowers(AUTH_MEMBER_ID, TARGET_MEMBER_ID, null, 50);
    }

    @Test
    void 팔로워_목록_조회시_cursor를_전달하면_그대로_서비스에_전달된다() throws Exception {
        String cursor = "2026-01-01T00:00:00Z/3";
        Pagination<MemberResDTO.FollowerItem> page = Pagination.<MemberResDTO.FollowerItem>builder()
                .data(List.of())
                .nextCursor(null)
                .hasNext(false)
                .pageSize(10)
                .build();
        when(memberQueryService.findFollowers(AUTH_MEMBER_ID, TARGET_MEMBER_ID, cursor, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/members/{memberId}/followers", TARGET_MEMBER_ID)
                        .param("cursor", cursor)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk());

        verify(memberQueryService).findFollowers(AUTH_MEMBER_ID, TARGET_MEMBER_ID, cursor, 10);
    }

    @Test
    void 팔로워_목록_조회시_잘못된_커서면_400을_반환한다() throws Exception {
        when(memberQueryService.findFollowers(eq(AUTH_MEMBER_ID), eq(TARGET_MEMBER_ID), eq("invalid-cursor"), eq(10)))
                .thenThrow(new MemberException(MemberErrorCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/v1/members/{memberId}/followers", TARGET_MEMBER_ID)
                        .param("cursor", "invalid-cursor")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_400_INVALID_CURSOR"));
    }

    @Test
    void 팔로잉_목록_조회에_성공하면_200과_FOLLOWING_FETCHED_응답을_반환한다() throws Exception {
        MemberResDTO.FollowingItem following = new MemberResDTO.FollowingItem(
                3L, "팔로잉", "이름", "key", Instant.parse("2026-01-01T00:00:00Z"), true);
        Pagination<MemberResDTO.FollowingItem> page = Pagination.<MemberResDTO.FollowingItem>builder()
                .data(List.of(following))
                .nextCursor(null)
                .hasNext(false)
                .pageSize(10)
                .build();
        when(memberQueryService.findFollowing(eq(AUTH_MEMBER_ID), eq(TARGET_MEMBER_ID), isNull(), eq(10))).thenReturn(page);

        mockMvc.perform(get("/api/v1/members/{memberId}/following", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200_FOLLOWING_FETCHED"))
                .andExpect(jsonPath("$.result.data[0].nickname").value("팔로잉"))
                .andExpect(jsonPath("$.result.data[0].isFollowing").value(true))
                .andExpect(jsonPath("$.result.hasNext").value(false));
    }

    @Test
    void 존재하지_않는_회원의_팔로잉_목록을_조회하면_404를_반환한다() throws Exception {
        when(memberQueryService.findFollowing(eq(AUTH_MEMBER_ID), eq(TARGET_MEMBER_ID), isNull(), eq(10)))
                .thenThrow(new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/members/{memberId}/following", TARGET_MEMBER_ID)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_404_MEMBER_NOT_FOUND"));
    }

    @Test
    void 팔로잉_목록_조회시_pageSize가_50을_초과하면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/members/{memberId}/following", TARGET_MEMBER_ID)
                        .param("pageSize", "51")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 팔로잉_목록_조회시_pageSize가_0이면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/members/{memberId}/following", TARGET_MEMBER_ID)
                        .param("pageSize", "0")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 팔로잉_목록_조회시_pageSize가_음수면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/members/{memberId}/following", TARGET_MEMBER_ID)
                        .param("pageSize", "-1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 팔로잉_목록_조회시_pageSize가_50이면_허용된다() throws Exception {
        Pagination<MemberResDTO.FollowingItem> page = Pagination.<MemberResDTO.FollowingItem>builder()
                .data(List.of())
                .nextCursor(null)
                .hasNext(false)
                .pageSize(50)
                .build();
        when(memberQueryService.findFollowing(eq(AUTH_MEMBER_ID), eq(TARGET_MEMBER_ID), isNull(), eq(50))).thenReturn(page);

        mockMvc.perform(get("/api/v1/members/{memberId}/following", TARGET_MEMBER_ID)
                        .param("pageSize", "50")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk());

        verify(memberQueryService).findFollowing(AUTH_MEMBER_ID, TARGET_MEMBER_ID, null, 50);
    }

    @Test
    void 팔로잉_목록_조회시_cursor를_전달하면_그대로_서비스에_전달된다() throws Exception {
        String cursor = "2026-01-01T00:00:00Z/3";
        Pagination<MemberResDTO.FollowingItem> page = Pagination.<MemberResDTO.FollowingItem>builder()
                .data(List.of())
                .nextCursor(null)
                .hasNext(false)
                .pageSize(10)
                .build();
        when(memberQueryService.findFollowing(AUTH_MEMBER_ID, TARGET_MEMBER_ID, cursor, 10)).thenReturn(page);

        mockMvc.perform(get("/api/v1/members/{memberId}/following", TARGET_MEMBER_ID)
                        .param("cursor", cursor)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk());

        verify(memberQueryService).findFollowing(AUTH_MEMBER_ID, TARGET_MEMBER_ID, cursor, 10);
    }

    @Test
    void 팔로잉_목록_조회시_잘못된_커서면_400을_반환한다() throws Exception {
        when(memberQueryService.findFollowing(eq(AUTH_MEMBER_ID), eq(TARGET_MEMBER_ID), eq("invalid-cursor"), eq(10)))
                .thenThrow(new MemberException(MemberErrorCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/v1/members/{memberId}/following", TARGET_MEMBER_ID)
                        .param("cursor", "invalid-cursor")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_400_INVALID_CURSOR"));
    }

    @Test
    void 프로필_이미지_업로드에_성공하면_200과_PROFILE_IMAGE_UPLOADED_응답을_반환한다() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "profile.webp", "image/webp", "webp-content".getBytes());
        MemberResDTO.ProfileImage result = new MemberResDTO.ProfileImage(
                "members/1/new.webp",
                "https://project.supabase.co/storage/v1/object/public/profile-images/members/1/new.webp"
        );
        when(memberCommandService.uploadProfileImage(eq(AUTH_MEMBER_ID), any())).thenReturn(result);

        mockMvc.perform(multipart("/api/v1/members/me/profile-image")
                        .file(image)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200_PROFILE_IMAGE_UPLOADED"))
                .andExpect(jsonPath("$.result.objectKey").value("members/1/new.webp"))
                .andExpect(jsonPath("$.result.imageUrl").value(result.imageUrl()));
    }

    @Test
    void Bearer_토큰과_CSRF_토큰_없이_프로필_이미지를_업로드하면_CSRF_검증에서_403으로_차단된다() throws Exception {
        // Bearer 인증 요청만 CSRF 검증에서 제외되므로(SecurityConfig 참고), Bearer 토큰이 없는
        // 상태 변경 요청은 인증 여부와 무관하게 CSRF 필터에서 먼저 403으로 막힌다.
        MockMultipartFile image = new MockMultipartFile(
                "image", "profile.webp", "image/webp", "webp-content".getBytes());

        mockMvc.perform(multipart("/api/v1/members/me/profile-image").file(image))
                .andExpect(status().isForbidden());
    }

    @Test
    void image_파트_없이_프로필_이미지_업로드를_요청하면_400을_반환한다() throws Exception {
        mockMvc.perform(multipart("/api/v1/members/me/profile-image")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 유효하지_않은_프로필_이미지면_400을_반환한다() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "profile.png", "image/png", "not-webp".getBytes());
        doThrow(new MemberException(MemberErrorCode.INVALID_PROFILE_IMAGE))
                .when(memberCommandService).uploadProfileImage(eq(AUTH_MEMBER_ID), any());

        mockMvc.perform(multipart("/api/v1/members/me/profile-image")
                        .file(image)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("MEMBER_400_INVALID_PROFILE_IMAGE"));
    }
}
