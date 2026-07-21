package com.example.plimap.domain.pin.service.query;

import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;

public interface PinQueryService {
    PinResponse.PinAvailability validatePinAvailability(PinRequest.PinAvailability request);
}
