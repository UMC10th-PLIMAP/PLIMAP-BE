package com.example.plimap.domain.pin.repository.query;

import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.dto.response.PinResponse;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PinQueryRepository{
    Optional<Double> findNearestActivePinWithin20m(double latitude, double longitude);

    Map<Long, PlacePinInfo> findPinInfosByPlaceIds(List<Long> placeIds);

    Pagination<PinResponse.Feed> findFeedListByMemberId(Long memberId, String cursor, Integer pageSize);

    Pagination<PinResponse.MyPin> findMyPinList(Long memberId, String cursor, Integer pageSize);
}
