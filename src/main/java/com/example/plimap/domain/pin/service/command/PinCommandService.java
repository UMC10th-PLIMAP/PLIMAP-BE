package com.example.plimap.domain.pin.service.command;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;

public interface PinCommandService {
    PinResponse.Summary createPin(Member currentMember, PinRequest.Create request);

    PinResponse.PinAvailability validatePinAvailability(PinRequest.PinAvailability request);
}
