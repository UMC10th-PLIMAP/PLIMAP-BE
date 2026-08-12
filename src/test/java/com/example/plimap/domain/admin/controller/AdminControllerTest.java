package com.example.plimap.domain.admin.controller;

import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.admin.service.command.AdminCommandService;
import com.example.plimap.domain.admin.service.query.AdminQueryService;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.auth.service.command.impl.CustomOAuthService;
import com.example.plimap.domain.auth.service.command.impl.OAuthFailureHandler;
import com.example.plimap.domain.auth.service.command.impl.OAuthSuccessHandler;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberRole;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.enums.SuspensionPeriod;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.pin.enums.PinReportFilter;
import com.example.plimap.domain.report.enums.ReportCategory;
import java.time.Instant;
import java.util.List;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import({
        SecurityConfig.class,
        CorsConfig.class,
        SecurityErrorResponseHandler.class,
        GlobalExceptionHandler.class
})
@ActiveProfiles("test")
class AdminControllerTest {

    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final Long MEMBER_ID = 1L;

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
    private MemberCommandService memberCommandService;

    @MockitoBean
    private AdminCommandService adminCommandService;

    @MockitoBean
    private AdminQueryService adminQueryService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    @MockitoBean
    private HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @BeforeEach
    void setUp() {
        when(jwtUtil.isValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.isAccessToken(ACCESS_TOKEN)).thenReturn(true);
        when(jwtUtil.getJti(ACCESS_TOKEN)).thenReturn("test-jti");
        when(tokenBlacklistService.isBlacklisted("test-jti")).thenReturn(false);
        when(jwtUtil.getMemberId(ACCESS_TOKEN)).thenReturn(MEMBER_ID);
    }

    @Test
    void 관리자_계정으로_조회하면_200과_내_정보를_반환한다() throws Exception {
        Member admin = Member.builder().nickname("운영자").role(MemberRole.ADMIN).build();
        ReflectionTestUtils.setField(admin, "id", MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(admin));

        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("ADMIN_200_ME_FETCHED"))
                .andExpect(jsonPath("$.result.id").value(MEMBER_ID))
                .andExpect(jsonPath("$.result.nickname").value("운영자"))
                .andExpect(jsonPath("$.result.role").value("ADMIN"));
    }

    @Test
    void 일반_회원으로_조회하면_403을_반환한다() throws Exception {
        Member user = Member.builder().nickname("일반회원").role(MemberRole.USER).build();
        ReflectionTestUtils.setField(user, "id", MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/v1/admin/me")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isForbidden());
    }

    @Test
    void 관리자가_PIN_신고를_반려하면_200을_반환한다() throws Exception {
        Member admin = Member.builder().nickname("운영자").role(MemberRole.ADMIN).build();
        ReflectionTestUtils.setField(admin, "id", MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/api/v1/admin/pins/10/penalty")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grantPenalty\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_PIN_PENALTY_REVIEWED"));

        verify(adminCommandService).reviewPinReport(eq(10L), eq(false));
    }

    @Test
    void 관리자가_PIN_최종_제재를_부여하면_200을_반환한다() throws Exception {
        mockAdminAuth();

        mockMvc.perform(post("/api/v1/admin/pins/10/sanctions")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\": 100, \"period\": \"THREE_DAYS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_PIN_SANCTION_GRANTED"));

        verify(adminCommandService).grantPinSanction(eq(10L), eq(100L), eq(SuspensionPeriod.THREE_DAYS));
    }

    @Test
    void 관리자가_프로필_최종_제재를_부여하면_200을_반환한다() throws Exception {
        mockAdminAuth();

        mockMvc.perform(post("/api/v1/admin/members/2/sanctions")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"OTHER\", \"detail\": \"반복 위반\", \"period\": \"PERMANENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_MEMBER_SANCTION_GRANTED"));

        verify(adminCommandService).grantMemberSanction(eq(2L), eq(ReportCategory.OTHER), eq("반복 위반"), eq(SuspensionPeriod.PERMANENT));
    }

    @Test
    void 관리자가_프로필_신고에_벌점을_부여하지_않으면_200을_반환한다() throws Exception {
        Member admin = Member.builder().nickname("운영자").role(MemberRole.ADMIN).build();
        ReflectionTestUtils.setField(admin, "id", MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(admin));

        mockMvc.perform(post("/api/v1/admin/members/2/penalty")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"grantPenalty\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_PROFILE_PENALTY_REVIEWED"));

        verify(adminCommandService).reviewProfileReport(eq(2L), eq(false));
    }

    @Test
    void 신고_누적_게시물_목록_조회에_성공하면_200을_반환한다() throws Exception {
        mockAdminAuth();
        AdminResDTO.ReportedPinItem item = new AdminResDTO.ReportedPinItem(
                1L, "제목", "장소", 2L, "작성자", MemberStatus.ACTIVE, 12, true, Instant.now(), List.of());
        when(adminQueryService.getReportedPins(PinReportFilter.ALL, 1, 10))
                .thenReturn(new AdminResDTO.ReportedPinPage(List.of(item), 1, 1, 10));

        mockMvc.perform(get("/api/v1/admin/pins/reports")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_REPORTED_PINS_FETCHED"))
                .andExpect(jsonPath("$.result.total").value(1))
                .andExpect(jsonPath("$.result.items[0].pinId").value(1))
                .andExpect(jsonPath("$.result.items[0].autoHidden").value(true));
    }

    @Test
    void 회원_목록_조회에_성공하면_200을_반환한다() throws Exception {
        mockAdminAuth();
        AdminResDTO.MemberSummary summary = new AdminResDTO.MemberSummary(
                2L, "닉네임", "이름", MemberStatus.ACTIVE, null, 0, null, Instant.now());
        when(adminQueryService.getMembers("검색어", MemberStatus.ACTIVE, 1, 10))
                .thenReturn(new AdminResDTO.MemberPage(List.of(summary), 1, 1, 10));

        mockMvc.perform(get("/api/v1/admin/members")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN)
                        .param("query", "검색어")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_MEMBERS_FETCHED"))
                .andExpect(jsonPath("$.result.items[0].id").value(2));
    }

    @Test
    void 회원_상세_조회에_성공하면_200을_반환한다() throws Exception {
        mockAdminAuth();
        AdminResDTO.MemberDetail detail = new AdminResDTO.MemberDetail(
                2L, "닉네임", "이름", "a@example.com", MemberStatus.ACTIVE, MemberRole.USER, null, 0, null, null, Instant.now());
        when(adminQueryService.getMemberDetail(2L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/members/2")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_MEMBER_DETAIL_FETCHED"))
                .andExpect(jsonPath("$.result.email").value("a@example.com"));
    }

    @Test
    void 회원_닉네임_강제_재생성에_성공하면_200을_반환한다() throws Exception {
        mockAdminAuth();
        AdminResDTO.MemberDetail detail = new AdminResDTO.MemberDetail(
                2L, "참새", "이름", null, MemberStatus.ACTIVE, MemberRole.USER, null, 0, null, null, Instant.now());
        when(adminCommandService.regenerateMemberNickname(2L)).thenReturn(detail);

        mockMvc.perform(post("/api/v1/admin/members/2/nickname/regenerate")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_MEMBER_NICKNAME_REGENERATED"))
                .andExpect(jsonPath("$.result.nickname").value("참새"));
    }

    @Test
    void 문의_목록_조회에_성공하면_200을_반환한다() throws Exception {
        mockAdminAuth();
        AdminResDTO.InquirySummary summary = new AdminResDTO.InquirySummary(
                1L, InquiryCategory.APP_BUG_OR_ERROR, "제목", 2L, "작성자", "user@example.com", Instant.now());
        when(adminQueryService.getInquiries(InquiryCategory.APP_BUG_OR_ERROR, null, 10))
                .thenReturn(new AdminResDTO.InquiryPage(List.of(summary), "next-cursor", true, 10));

        mockMvc.perform(get("/api/v1/admin/inquiries")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN)
                        .param("category", "APP_BUG_OR_ERROR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_INQUIRIES_FETCHED"))
                .andExpect(jsonPath("$.result.items[0].id").value(1))
                .andExpect(jsonPath("$.result.items[0].memberNickname").value("작성자"))
                .andExpect(jsonPath("$.result.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.result.hasNext").value(true));
    }

    @Test
    void 문의_상세_조회에_성공하면_200을_반환한다() throws Exception {
        mockAdminAuth();
        AdminResDTO.InquiryDetail detail = new AdminResDTO.InquiryDetail(
                1L, InquiryCategory.OTHER, "제목", "내용", null, null, "guest@example.com", Instant.now());
        when(adminQueryService.getInquiryDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/inquiries/1")
                        .header("Authorization", "Bearer " + ACCESS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("ADMIN_200_INQUIRY_DETAIL_FETCHED"))
                .andExpect(jsonPath("$.result.content").value("내용"))
                .andExpect(jsonPath("$.result.memberId").value(org.hamcrest.Matchers.nullValue()));
    }

    private void mockAdminAuth() {
        Member admin = Member.builder().nickname("운영자").role(MemberRole.ADMIN).build();
        ReflectionTestUtils.setField(admin, "id", MEMBER_ID);
        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(admin));
    }
}
