package com.example.plimap.domain.pin.service.query;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.enums.PinSortType;
import com.example.plimap.domain.place.entity.Place;

import java.util.List;
import java.util.Map;

public interface PinQueryService {

    Pin getActivePin(Long pinId);

    PinResponse.PinAvailability validatePinAvailability(PinRequest.PinAvailability request);

    Map<Long, PlacePinInfo> findPinInfosByPlaceIds(List<Long> placeIds);

    Pagination<PinResponse.Feed> findFeedListByMemberId(Long memberId, String cursor, Integer pageSize);

    Pagination<PinResponse.MyPin> findMyPinList(Long memberId, String cursor, Integer pageSize);

    Boolean validatePlacePinAccessByMember(Member member, Place place);

    Pagination<PinResponse.PinDetail> findPinListByPlaceTrackIdAndSortType(Long memberId, String cursor, Integer pageSize, PinSortType pinSortType, Long placeTrackId);

    PinResponse.PinPreview getPinPreview(Long pinId);
}
