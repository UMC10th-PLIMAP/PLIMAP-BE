package com.example.plimap.domain.admin.service.query.impl;

import com.example.plimap.domain.admin.dto.response.AdminResponse;
import com.example.plimap.domain.auth.service.query.AuthQueryService;
import com.example.plimap.domain.inquiry.dto.Pagination;
import com.example.plimap.domain.inquiry.entity.Inquiry;
import com.example.plimap.domain.inquiry.enums.InquiryCategory;
import com.example.plimap.domain.inquiry.service.query.InquiryQueryService;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.MemberStatus;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.pin.dto.ReportedPinInfo;
import com.example.plimap.domain.pin.enums.PinReportFilter;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.report.dto.ReportReason;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.service.query.ReportQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminQueryServiceImplTest {

    private final PinQueryService pinQueryService = mock(PinQueryService.class);
    private final MemberQueryService memberQueryService = mock(MemberQueryService.class);
    private final ReportQueryService reportQueryService = mock(ReportQueryService.class);
    private final AuthQueryService authQueryService = mock(AuthQueryService.class);
    private final InquiryQueryService inquiryQueryService = mock(InquiryQueryService.class);
    private final AdminQueryServiceImpl adminQueryService = new AdminQueryServiceImpl(
            pinQueryService, memberQueryService, reportQueryService, authQueryService, inquiryQueryService);

    @Test
    void 신고_누적_핀_목록에_사유와_자동숨김_여부를_함께_담는다() {
        Instant now = Instant.now();
        ReportedPinInfo autoHiddenPin = new ReportedPinInfo(
                1L, "제목1", "장소1", 10L, "작성자1", MemberStatus.ACTIVE, 10, now);
        ReportedPinInfo belowThresholdPin = new ReportedPinInfo(
                2L, "제목2", "장소2", 20L, "작성자2", MemberStatus.ACTIVE, 3, now);
        Page<ReportedPinInfo> page = new PageImpl<>(List.of(autoHiddenPin, belowThresholdPin), PageRequest.of(0, 10), 2);
        when(pinQueryService.findReportedPins(eq(PinReportFilter.ALL), any(Pageable.class))).thenReturn(page);

        ReportReason reason = new ReportReason(100L, 1L, ReportCategory.OTHER, "상세", "신고자", now);
        when(reportQueryService.findReasonsByPinIds(List.of(1L, 2L)))
                .thenReturn(Map.of(1L, List.of(reason)));

        AdminResponse.ReportedPinPage result = adminQueryService.getReportedPins(PinReportFilter.ALL, 1, 10);

        assertThat(result.total()).isEqualTo(2);
        assertThat(result.items()).hasSize(2);

        AdminResponse.ReportedPinItem first = result.items().get(0);
        assertThat(first.pinId()).isEqualTo(1L);
        assertThat(first.autoHidden()).isTrue();
        assertThat(first.reasons()).hasSize(1);
        assertThat(first.reasons().get(0).reportId()).isEqualTo(100L);
        assertThat(first.reasons().get(0).reporterNickname()).isEqualTo("신고자");

        AdminResponse.ReportedPinItem second = result.items().get(1);
        assertThat(second.autoHidden()).isFalse();
        assertThat(second.reasons()).isEmpty();
    }

    @Test
    void 회원_목록을_페이지_정보와_함께_반환한다() {
        Member member = Member.builder().nickname("닉네임").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        Page<Member> page = new PageImpl<>(List.of(member), PageRequest.of(0, 10), 1);
        when(memberQueryService.searchMembers(eq("검색어"), eq(MemberStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(page);

        AdminResponse.MemberPage result = adminQueryService.getMembers("검색어", MemberStatus.ACTIVE, 1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.items()).extracting(AdminResponse.MemberSummary::id).containsExactly(1L);
    }

    @Test
    void 회원_상세에_이메일을_함께_담는다() {
        Member member = Member.builder().nickname("닉네임").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberQueryService.getMemberById(1L)).thenReturn(member);
        when(authQueryService.findEmailByMemberId(1L)).thenReturn(Optional.of("a@example.com"));

        AdminResponse.MemberDetail result = adminQueryService.getMemberDetail(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.email()).isEqualTo("a@example.com");
    }

    @Test
    void 이메일이_없는_회원의_상세_조회시_email은_null이다() {
        Member member = Member.builder().nickname("닉네임").build();
        ReflectionTestUtils.setField(member, "id", 1L);
        when(memberQueryService.getMemberById(1L)).thenReturn(member);
        when(authQueryService.findEmailByMemberId(1L)).thenReturn(Optional.empty());

        AdminResponse.MemberDetail result = adminQueryService.getMemberDetail(1L);

        assertThat(result.email()).isNull();
    }

    @Test
    void 문의_목록을_커서_페이지_정보와_함께_반환한다() {
        Inquiry inquiry = Inquiry.create(null, InquiryCategory.OTHER, "제목", "내용", "guest@example.com");
        ReflectionTestUtils.setField(inquiry, "id", 1L);
        Pagination<Inquiry> page = Pagination.<Inquiry>builder()
                .data(List.of(inquiry))
                .nextCursor("next-cursor")
                .hasNext(true)
                .pageSize(10)
                .build();
        when(inquiryQueryService.getInquiries(InquiryCategory.OTHER, "cursor", 10)).thenReturn(page);

        AdminResponse.InquiryPage result = adminQueryService.getInquiries(InquiryCategory.OTHER, "cursor", 10);

        assertThat(result.nextCursor()).isEqualTo("next-cursor");
        assertThat(result.hasNext()).isTrue();
        assertThat(result.items()).extracting(AdminResponse.InquirySummary::id).containsExactly(1L);
        assertThat(result.items().get(0).memberId()).isNull();
    }

    @Test
    void 문의_상세를_반환한다() {
        Member member = Member.builder().nickname("작성자").build();
        ReflectionTestUtils.setField(member, "id", 5L);
        Inquiry inquiry = Inquiry.create(member, InquiryCategory.APP_BUG_OR_ERROR, "제목", "내용", "user@example.com");
        ReflectionTestUtils.setField(inquiry, "id", 1L);
        when(inquiryQueryService.getInquiry(1L)).thenReturn(inquiry);

        AdminResponse.InquiryDetail result = adminQueryService.getInquiryDetail(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.memberId()).isEqualTo(5L);
        assertThat(result.memberNickname()).isEqualTo("작성자");
        assertThat(result.content()).isEqualTo("내용");
    }
}
