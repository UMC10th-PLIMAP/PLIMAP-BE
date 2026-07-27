package com.example.plimap.domain.pin.service.query.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.enums.AvailabilityStatus;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.pin.service.query.PinQueryService;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PinQueryServiceImpl implements PinQueryService {

    private final PinLocationValidator pinLocationValidator;
    private final PinQueryRepository pinQueryRepository;
    private final PinRepository pinRepository;

    @Override
    public Pin getActivePin(Long pinId) {
        return pinRepository.findByIdAndDeletedAtIsNull(pinId)
                .orElseThrow(() -> new PinException(PinErrorCode.PIN_NOT_FOUND));
    }

    @Override
    public PinResponse.PinAvailability validatePinAvailability(PinRequest.PinAvailability request) {
        // 현위치와 장소 사이 거리가 500m 이하인지 검증
        double distanceFromUserMeters = pinLocationValidator.calculateDistance(request.userLatitude(), request.userLongitude(), request.latitude(), request.longitude());
        if (distanceFromUserMeters > 500) {
            return PinConverter.toPinAvailability(AvailabilityStatus.OUT_OF_RANGE, false, distanceFromUserMeters, null);
        }

        // 20m 이내에 PIN 존재하는지 검증
        Double nearestPinDistanceMeters = pinQueryRepository.findNearestActivePinWithin20m(request.latitude(), request.longitude()).orElse(null);
        if (nearestPinDistanceMeters != null) {
            return PinConverter.toPinAvailability(AvailabilityStatus.TOO_CLOSE_TO_PIN, false, distanceFromUserMeters, nearestPinDistanceMeters);
        }

        return PinConverter.toPinAvailability(AvailabilityStatus.CREATABLE_NEW_PLACE, true, distanceFromUserMeters, null);
    }

    @Override
    public Map<Long, PlacePinInfo> findPinInfosByPlaceIds(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return pinQueryRepository.findPinInfosByPlaceIds(placeIds);
    }

    @Override
    public Pagination<PinResponse.Feed> findFeedListByMemberId(Long memberId, String cursor, Integer pageSize) {
        return pinQueryRepository.findFeedListByMemberId(memberId, cursor, pageSize);
    }

    @Override
    public Pagination<PinResponse.MyPin> findMyPinList(Long memberId, String cursor, Integer pageSize) {
        return pinQueryRepository.findMyPinList(memberId, cursor, pageSize);
    }

    @Override
    public Pagination<PinResponse.PinDetail> findPinListByPlaceTrackId(Long memberId, String cursor, Integer pageSize, Long placeTrackId) {
        return pinQueryRepository.findPinListByPlaceTrackId(memberId, cursor, pageSize, placeTrackId);
    }
}
