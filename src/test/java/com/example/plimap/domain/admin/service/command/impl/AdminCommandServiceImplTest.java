package com.example.plimap.domain.admin.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.domain.member.service.query.MemberQueryService;
import com.example.plimap.domain.notification.service.command.NotificationCommandService;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.report.service.command.ReportCommandService;
import com.example.plimap.domain.track.service.command.PlaceTrackCommandService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private PlaceTrackCommandService placeTrackCommandService;

    @Test
    void 핀_신고에_벌점을_부여하지_않으면_신고누적만_초기화한다() {
        adminCommandService.reviewPinReport(PIN_ID, false);

        verify(pinCommandService).resetPinReportCount(PIN_ID);
        verify(pinQueryService, never()).getActivePin(PIN_ID);
        verifyNoInteractions(memberCommandService);
    }

    @Test
    void 핀_신고에_벌점을_부여하면_핀을_삭제하고_작성자_벌점을_올린다() {
        Member owner = Member.builder().nickname("작성자").build();
        ReflectionTestUtils.setField(owner, "id", MEMBER_ID);
        Pin pin = Pin.builder().member(owner).build();
        when(pinQueryService.getActivePin(PIN_ID)).thenReturn(pin);
        when(memberCommandService.increasePenaltyPoint(MEMBER_ID)).thenReturn(false);

        adminCommandService.reviewPinReport(PIN_ID, true);

        verify(pinCommandService).penalizePin(PIN_ID);
        verify(memberCommandService).increasePenaltyPoint(MEMBER_ID);
        verify(pinCommandService, never()).hardDeleteLikesByMember(any());
        verify(pinQueryService, never()).findPinIdsLikedByMember(any());
        verifyNoInteractions(notificationCommandService, reportCommandService, placeTrackCommandService);
    }

    @Test
    void 핀_신고_벌점_부여로_4점에_도달하면_자동탈퇴_캐스케이드가_실행된다() {
        Long likedPinId = 99L;
        Member owner = Member.builder().nickname("작성자").build();
        ReflectionTestUtils.setField(owner, "id", MEMBER_ID);
        Pin pin = Pin.builder().member(owner).build();
        when(pinQueryService.getActivePin(PIN_ID)).thenReturn(pin);
        when(memberCommandService.increasePenaltyPoint(MEMBER_ID)).thenReturn(true);
        when(pinQueryService.findAllPinIdsByMemberId(MEMBER_ID)).thenReturn(List.of(PIN_ID));
        when(pinQueryService.findPinIdsLikedByMember(MEMBER_ID)).thenReturn(List.of(likedPinId));

        adminCommandService.reviewPinReport(PIN_ID, true);

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
    void 프로필_신고에_벌점을_부여하지_않으면_신고누적만_초기화한다() {
        adminCommandService.reviewProfileReport(MEMBER_ID, false);

        verify(memberCommandService).resetReportCount(MEMBER_ID);
        verify(memberCommandService, never()).increasePenaltyPoint(MEMBER_ID);
        verifyNoInteractions(memberQueryService);
    }

    @Test
    void 프로필_신고에_벌점을_부여하면_닉네임을_치환하고_벌점을_올린다() {
        when(memberQueryService.pickAvailablePenaltyNickname()).thenReturn("참새");
        when(memberCommandService.increasePenaltyPoint(MEMBER_ID)).thenReturn(false);

        adminCommandService.reviewProfileReport(MEMBER_ID, true);

        verify(memberCommandService).replacePenalizedNickname(MEMBER_ID, "참새");
        verify(memberCommandService).increasePenaltyPoint(MEMBER_ID);
        verifyNoInteractions(notificationCommandService, reportCommandService, placeTrackCommandService, pinCommandService);
    }

    @Test
    void 프로필_신고_벌점_부여로_4점에_도달하면_자동탈퇴_캐스케이드가_실행된다() {
        Long likedPinId = 99L;
        when(memberQueryService.pickAvailablePenaltyNickname()).thenReturn("참새");
        when(memberCommandService.increasePenaltyPoint(MEMBER_ID)).thenReturn(true);
        when(pinQueryService.findAllPinIdsByMemberId(MEMBER_ID)).thenReturn(List.of());
        when(pinQueryService.findPinIdsLikedByMember(MEMBER_ID)).thenReturn(List.of(likedPinId));

        adminCommandService.reviewProfileReport(MEMBER_ID, true);

        verify(notificationCommandService).deleteByMemberId(MEMBER_ID);
        verify(pinCommandService).decreaseLikeCount(likedPinId);
        verify(pinCommandService).hardDeleteLikesByMember(MEMBER_ID);
        verify(pinCommandService).hardDeleteAllByMember(MEMBER_ID);
        verify(placeTrackCommandService).hardDeleteLikesByMember(MEMBER_ID);
        verify(reportCommandService).deleteReportsAgainstMember(MEMBER_ID);
        verify(reportCommandService).deleteReportsByReporter(MEMBER_ID);
    }
}
