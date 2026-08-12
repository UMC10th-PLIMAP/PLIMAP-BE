package com.example.plimap.domain.admin.service.command.impl;

import com.example.plimap.domain.admin.dto.response.AdminResDTO;
import com.example.plimap.domain.admin.exception.AdminException;
import com.example.plimap.domain.auth.service.query.AuthQueryService;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.enums.SuspensionPeriod;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.notification.service.command.NotificationCommandService;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.report.dto.ReportReason;
import com.example.plimap.domain.report.enums.ReportCategory;
import com.example.plimap.domain.report.exception.ReportException;
import com.example.plimap.domain.report.service.command.ReportCommandService;
import com.example.plimap.domain.report.service.query.ReportQueryService;
import com.example.plimap.domain.track.service.command.PlaceTrackCommandService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCommandServiceImplTest {

    private static final Long PIN_ID = 10L;
    private static final Long MEMBER_ID = 1L;
    private static final Long REPORT_ID = 100L;

    @InjectMocks
    private AdminCommandServiceImpl adminCommandService;

    @Mock
    private PinQueryService pinQueryService;

    @Mock
    private PinCommandService pinCommandService;

    @Mock
    private MemberQueryService memberQueryService;

    @Mock
    private MemberCommandService memberCommandService;

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private ReportCommandService reportCommandService;

    @Mock
    private ReportQueryService reportQueryService;

    @Mock
    private PlaceTrackCommandService placeTrackCommandService;

    @Mock
    private AuthQueryService authQueryService;

    @Test
    void 핀_신고를_반려하면_신고누적만_초기화한다() {
        adminCommandService.reviewPinReport(PIN_ID, false);

        verify(pinCommandService).resetPinReportCount(PIN_ID);
        verify(reportCommandService).markPinReportsReviewed(PIN_ID);
        verify(pinQueryService, never()).getActivePin(PIN_ID);
        verifyNoInteractions(memberCommandService);
    }

    @Test
    void 핀_신고_검토_API에_벌점_부여를_요청하면_거부된다() {
        assertThatThrownBy(() -> adminCommandService.reviewPinReport(PIN_ID, true))
                .isInstanceOf(AdminException.class);

        verifyNoInteractions(pinCommandService, memberCommandService, reportCommandService);
    }

    @Test
    void 프로필_신고를_반려하면_신고누적만_초기화한다() {
        adminCommandService.reviewProfileReport(MEMBER_ID, false);

        verify(memberCommandService).resetReportCount(MEMBER_ID);
        verifyNoInteractions(memberQueryService);
    }

    @Test
    void 프로필_신고_검토_API에_벌점_부여를_요청하면_거부된다() {
        assertThatThrownBy(() -> adminCommandService.reviewProfileReport(MEMBER_ID, true))
                .isInstanceOf(AdminException.class);

        verifyNoInteractions(memberCommandService, memberQueryService);
    }

    @Test
    void 핀_최종_제재를_부여하면_핀을_삭제하고_지목한_신고_사유로_작성자_벌점을_올린다() {
        Member owner = Member.builder().nickname("작성자").build();
        ReflectionTestUtils.setField(owner, "id", MEMBER_ID);
        Pin pin = Pin.builder().member(owner).build();
        ReportReason reason = new ReportReason(
                REPORT_ID, PIN_ID, ReportCategory.ABUSE_OR_HATE_SPEECH, null, "신고자", Instant.now());
        when(reportQueryService.getReasonById(REPORT_ID)).thenReturn(reason);
        when(pinQueryService.getActivePin(PIN_ID)).thenReturn(pin);
        when(memberCommandService.applySanction(MEMBER_ID, SuspensionPeriod.THREE_DAYS, ReportCategory.ABUSE_OR_HATE_SPEECH, null))
                .thenReturn(false);

        adminCommandService.grantPinSanction(PIN_ID, REPORT_ID, SuspensionPeriod.THREE_DAYS);

        verify(pinCommandService).penalizePin(PIN_ID);
        verify(memberCommandService).applySanction(MEMBER_ID, SuspensionPeriod.THREE_DAYS, ReportCategory.ABUSE_OR_HATE_SPEECH, null);
        verifyNoInteractions(notificationCommandService, placeTrackCommandService);
    }

    @Test
    void 핀_최종_제재_시_다른_핀의_신고를_지목하면_거부된다() {
        ReportReason reasonForOtherPin = new ReportReason(
                REPORT_ID, 999L, ReportCategory.OTHER, "상세", "신고자", Instant.now());
        when(reportQueryService.getReasonById(REPORT_ID)).thenReturn(reasonForOtherPin);

        assertThatThrownBy(() -> adminCommandService.grantPinSanction(PIN_ID, REPORT_ID, SuspensionPeriod.ONE_DAY))
                .isInstanceOf(ReportException.class);

        verifyNoInteractions(pinQueryService, pinCommandService, memberCommandService);
    }

    @Test
    void 핀_최종_제재로_4점에_도달하면_자동탈퇴_캐스케이드가_실행된다() {
        Long likedPinId = 99L;
        Member owner = Member.builder().nickname("작성자").build();
        ReflectionTestUtils.setField(owner, "id", MEMBER_ID);
        Pin pin = Pin.builder().member(owner).build();
        ReportReason reason = new ReportReason(
                REPORT_ID, PIN_ID, ReportCategory.OBSCENE_OR_HARMFUL, null, "신고자", Instant.now());
        when(reportQueryService.getReasonById(REPORT_ID)).thenReturn(reason);
        when(pinQueryService.getActivePin(PIN_ID)).thenReturn(pin);
        when(memberCommandService.applySanction(MEMBER_ID, SuspensionPeriod.FIVE_DAYS, ReportCategory.OBSCENE_OR_HARMFUL, null))
                .thenReturn(true);
        when(pinQueryService.findAllPinIdsByMemberId(MEMBER_ID)).thenReturn(List.of(PIN_ID));
        when(pinQueryService.findPinIdsLikedByMember(MEMBER_ID)).thenReturn(List.of(likedPinId));

        adminCommandService.grantPinSanction(PIN_ID, REPORT_ID, SuspensionPeriod.FIVE_DAYS);

        InOrder order = inOrder(notificationCommandService, reportCommandService, pinCommandService, placeTrackCommandService);
        order.verify(notificationCommandService).deleteByMemberId(MEMBER_ID);
        order.verify(reportCommandService).deleteReportsByPinIds(List.of(PIN_ID));
        order.verify(reportCommandService).deleteReportsAgainstMember(MEMBER_ID);
        order.verify(reportCommandService).deleteReportsByReporter(MEMBER_ID);
        order.verify(pinCommandService).decreaseLikeCount(likedPinId);
        order.verify(pinCommandService).hardDeleteLikesByMember(MEMBER_ID);
        order.verify(pinCommandService).hardDeleteAllByMember(MEMBER_ID);
        order.verify(placeTrackCommandService).hardDeleteLikesByMember(MEMBER_ID);
    }

    @Test
    void 프로필_최종_제재를_부여하면_닉네임을_치환하고_작성한_사유로_벌점을_올린다() {
        when(memberQueryService.pickAvailablePenaltyNickname()).thenReturn("참새");
        when(memberCommandService.applySanction(MEMBER_ID, SuspensionPeriod.ONE_DAY, ReportCategory.COMMERCIAL_OR_PROMOTIONAL, null))
                .thenReturn(false);

        adminCommandService.grantMemberSanction(MEMBER_ID, SuspensionPeriod.ONE_DAY, ReportCategory.COMMERCIAL_OR_PROMOTIONAL, null);

        verify(memberCommandService).replacePenalizedNickname(MEMBER_ID, "참새");
        verify(memberCommandService).applySanction(MEMBER_ID, SuspensionPeriod.ONE_DAY, ReportCategory.COMMERCIAL_OR_PROMOTIONAL, null);
        verifyNoInteractions(notificationCommandService, reportCommandService, placeTrackCommandService, pinCommandService);
    }

    // 사유(category/detail) 정합성 검증은 AdminReqDTO.MemberSanctionDecision의 @AssertTrue로 이동됨
    // (ReportRequest.Create.isDetailValid()와 동일 패턴) — AdminControllerTest에서 검증한다.

    @Test
    void 프로필_최종_제재를_영구로_선택하면_4점_미만이어도_즉시_탈퇴_캐스케이드가_실행된다() {
        Long likedPinId = 99L;
        when(memberQueryService.pickAvailablePenaltyNickname()).thenReturn("참새");
        when(memberCommandService.applySanction(MEMBER_ID, SuspensionPeriod.PERMANENT, ReportCategory.OTHER, "심각한 위반"))
                .thenReturn(true);
        when(pinQueryService.findAllPinIdsByMemberId(MEMBER_ID)).thenReturn(List.of());
        when(pinQueryService.findPinIdsLikedByMember(MEMBER_ID)).thenReturn(List.of(likedPinId));

        adminCommandService.grantMemberSanction(MEMBER_ID, SuspensionPeriod.PERMANENT, ReportCategory.OTHER, "심각한 위반");

        verify(notificationCommandService).deleteByMemberId(MEMBER_ID);
        verify(pinCommandService).decreaseLikeCount(likedPinId);
        verify(pinCommandService).hardDeleteLikesByMember(MEMBER_ID);
        verify(pinCommandService).hardDeleteAllByMember(MEMBER_ID);
        verify(placeTrackCommandService).hardDeleteLikesByMember(MEMBER_ID);
        verify(reportCommandService).deleteReportsAgainstMember(MEMBER_ID);
        verify(reportCommandService).deleteReportsByReporter(MEMBER_ID);
    }

    @Test
    void 닉네임_강제_재생성은_벌점_없이_닉네임만_치환한다() {
        Member updated = Member.builder().nickname("참새").build();
        ReflectionTestUtils.setField(updated, "id", MEMBER_ID);
        when(memberQueryService.pickAvailablePenaltyNickname()).thenReturn("참새");
        when(memberQueryService.getMemberById(MEMBER_ID)).thenReturn(updated);
        when(authQueryService.findEmailByMemberId(MEMBER_ID)).thenReturn(Optional.of("a@example.com"));

        AdminResDTO.MemberDetail result = adminCommandService.regenerateMemberNickname(MEMBER_ID);

        verify(memberCommandService).regenerateNickname(MEMBER_ID, "참새");
        verify(memberCommandService, never()).applySanction(any(), any(), any(), any());
        assertThat(result.nickname()).isEqualTo("참새");
        assertThat(result.email()).isEqualTo("a@example.com");
        verifyNoInteractions(notificationCommandService, reportCommandService, placeTrackCommandService, pinCommandService);
    }
}
