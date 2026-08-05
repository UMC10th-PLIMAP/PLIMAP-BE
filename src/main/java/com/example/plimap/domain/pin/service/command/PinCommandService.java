package com.example.plimap.domain.pin.service.command;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;

public interface PinCommandService {
    PinResponse.Summary createPin(Member currentMember, PinRequest.Create request);

    PinResponse.UpdatedPin updatePin(Member currentMember, PinRequest.Update request, Long pinId);

    void deletePin(Member currentMember, Long pinId);

    PinResponse.LikeCount createPinLike(Member currentMember, Long pinId);

    PinResponse.LikeCount deletePinLike(Member currentMember, Long pinId);

    void increaseReportCount(Long pinId);

    void decreaseReportCount(Long pinId);

    void penalizePin(Long pinId);

    void resetPinReportCount(Long pinId);

    void hardDeleteAllByMember(Long memberId);
}
