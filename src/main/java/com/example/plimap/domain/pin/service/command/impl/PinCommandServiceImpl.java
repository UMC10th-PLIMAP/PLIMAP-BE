package com.example.plimap.domain.pin.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.PinTag;
import com.example.plimap.domain.pin.entity.Tag;
import com.example.plimap.domain.pin.enums.AvailabilityStatus;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.exception.TagErrorCode;
import com.example.plimap.domain.pin.exception.TagException;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.PinTagRepository;
import com.example.plimap.domain.pin.repository.TagRepository;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.repository.PlaceRepository;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.domain.track.dto.request.TrackCommand;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.service.command.TrackCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PinCommandServiceImpl implements PinCommandService {

    private final PlaceQueryService placeQueryService;
    private final TrackCommandService trackCommandService;
    private final PinRepository pinRepository;
    private final TagRepository tagRepository;
    private final PinTagRepository pinTagRepository;
    private final PinLocationValidator pinLocationValidator;
    private final PinQueryRepository pinQueryRepository;

    @Override
    @Transactional
    public PinResponse.Summary createPin(Member currentMember, PinRequest.Create request) {

        // 장소 조회
        Place place = placeQueryService.getActivePlace(request.placeId());

        // 현위치와 장소 사이 거리가 500m 이하인지 검증
        pinLocationValidator.validateWithin500m(request.userLatitude(), request.userLongitude(), place);

        // 노래 등록/조회
        PlaceTrack placeTrack = trackCommandService.getOrCreatePlaceTrack(place, TrackCommand.Create.builder().itunesTrackId(request.itunesTrackId()).build());

        // 핀 등록
        Pin pin = Pin.create(currentMember, place, placeTrack, request);
        validatePinExistsByMemberAndPlace(currentMember, place);
        pinRepository.save(pin);

        // 핀 태그 등록
        if (request.tags().size() > 4) {
            throw new TagException(TagErrorCode.TAG_SIZE_OVER_RANGE);
        }
        List<Tag> tags = tagRepository.findAllByNameIn(request.tags());
        if (tags.size() != request.tags().size()) {
            throw new TagException(TagErrorCode.TAG_NOT_FOUND);
        }
        tags.sort(Comparator.comparing(Tag::getDisplayOrder));

        List<PinTag> pinTags = new ArrayList<>();

        for (int i = 0; i < tags.size(); i++) {
            PinTag pinTag = PinTag.create(pin, tags.get(i), (short) (i));
            pinTags.add(pinTag);
        }

        pinTagRepository.saveAll(pinTags);

        return PinConverter.toSummary(currentMember, pin, placeTrack.getTrack(), place.getId());
    }

    private void validatePinExistsByMemberAndPlace(Member member, Place place) {
        if (pinRepository.existsByMemberAndPlaceAndDeletedAtIsNull(member, place)) {
            throw new PinException(PinErrorCode.MEMBER_PIN_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional(readOnly = true)
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
}
