package com.example.plimap.domain.pin.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.PinTag;
import com.example.plimap.domain.pin.entity.Tag;
import com.example.plimap.domain.pin.exception.PinErrorCode;
import com.example.plimap.domain.pin.exception.PinException;
import com.example.plimap.domain.pin.exception.TagErrorCode;
import com.example.plimap.domain.pin.exception.TagException;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.PinTagRepository;
import com.example.plimap.domain.pin.repository.TagRepository;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.exception.PlaceErrorCode;
import com.example.plimap.domain.place.exception.PlaceException;
import com.example.plimap.domain.place.repository.PlaceRepository;
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

    private final PlaceRepository placeRepository;
    private final TrackCommandService trackCommandService;
    private final PinRepository pinRepository;
    private final TagRepository tagRepository;
    private final PinTagRepository pinTagRepository;
    private final PinLocationValidator pinLocationValidator;

    @Override
    @Transactional
    public PinResponse.Summary createPin(Member currentMember, PinRequest.Create request) {

        // 장소 조회
        Place place = placeRepository.findById(request.placeId())
                .orElseThrow(() -> new PlaceException(PlaceErrorCode.PLACE_NOT_FOUND));

        // 현위치와 장소 사이 거리가 500m 이하인지 검증
        pinLocationValidator.validateWithin500m(request.userLatitude(), request.userLongitude(), place);

        // 노래 등록/조회
        PlaceTrack placeTrack = trackCommandService.getOrCreatePlaceTrack(place, TrackCommand.Create.builder().itunesTrackId(request.itunesTrackId()).build());

        // 핀 등록
        Pin pin = Pin.create(currentMember, place, placeTrack, request);
        validatePinExistsByMemberAndPlace(currentMember, place);
        pinRepository.save(pin);

        // 핀 태그 등록
        List<Tag> tags = tagRepository.findAllByNameIn(request.tags());
        if (tags.size() != request.tags().size()) {
            throw new TagException(TagErrorCode.TAG_NOT_FOUND);
        }
        tags.sort(Comparator.comparing(Tag::getDisplayOrder));

        List<PinTag> pinTags = new ArrayList<>();

        for (int i = 0; i < tags.size(); i++) {
            PinTag pinTag = PinTag.create(pin, tags.get(i), (short) (i + 1));
            pinTags.add(pinTag);
        }

        pinTagRepository.saveAll(pinTags);

        return PinConverter.toSummary(currentMember, pin, placeTrack.getTrack(), place.getId());
    }

    private void validatePinExistsByMemberAndPlace(Member member, Place place) {
        if (pinRepository.existsByMemberAndPlace(member, place)) {
            throw new PinException(PinErrorCode.MEMBER_PIN_ALREADY_EXISTS);
        }
    }
}
