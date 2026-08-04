package com.example.plimap.domain.pin.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.PinLike;
import com.example.plimap.domain.pin.entity.PinTag;
import com.example.plimap.domain.pin.entity.Tag;
import com.example.plimap.domain.pin.event.PinCreatedEvent;
import com.example.plimap.domain.pin.event.PinLikedEvent;
import com.example.plimap.domain.pin.exception.*;
import com.example.plimap.domain.pin.repository.PinLikeRepository;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.PinTagRepository;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.service.query.TagQueryService;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.domain.track.dto.request.TrackCommand;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.service.command.TrackCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class PinCommandServiceImpl implements PinCommandService {

    private final PlaceQueryService placeQueryService;
    private final TrackCommandService trackCommandService;
    private final PinRepository pinRepository;
    private final PinTagRepository pinTagRepository;
    private final PinLocationValidator pinLocationValidator;
    private final TagQueryService tagQueryService;
    private final PinLikeRepository pinLikeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
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
        List<PinTag> pinTags = toPinTags(request.tags(), pin);

        pinTagRepository.saveAll(pinTags);

        eventPublisher.publishEvent(new PinCreatedEvent(pin.getId(), currentMember.getId()));

        return PinConverter.toSummary(currentMember, pin, placeTrack.getTrack(), place.getId());
    }

    private List<PinTag> toPinTags(List<String> stringTags, Pin pin) {
        if (stringTags.size() > 4) {
            throw new TagException(TagErrorCode.TAG_SIZE_OVER_RANGE);
        }
        List<Tag> tags = tagQueryService.getTagsByNames(stringTags);
        tags.sort(Comparator.comparing(Tag::getDisplayOrder));

        List<PinTag> pinTags = new ArrayList<>();

        for (int i = 0; i < tags.size(); i++) {
            PinTag pinTag = PinTag.create(pin, tags.get(i), (short) (i));
            pinTags.add(pinTag);
        }
        return pinTags;
    }

    private void validatePinExistsByMemberAndPlace(Member member, Place place) {
        if (pinRepository.existsByMemberAndPlaceAndDeletedAtIsNull(member, place)) {
            throw new PinException(PinErrorCode.MEMBER_PIN_ALREADY_EXISTS);
        }
    }

    @Override
    public PinResponse.UpdatedPin updatePin(Member currentMember, PinRequest.Update request, Long pinId) {
        Pin pin = getPin(pinId);
        validateMemberAuthorization(currentMember.getId(), pin.getMember().getId());

        if (request.introduction() == null && request.tags() == null && request.feedOpen() == null) {
            throw new PinException(PinErrorCode.PIN_NOT_CHANGED);
        }

        if (request.introduction() != null) {
            pin.updateIntroduction(request.introduction());
        }

        if (request.tags() != null) {
            List<PinTag> pinTags = toPinTags(request.tags(), pin);
            pin.getPinTagList().clear();
            pinRepository.flush();
            pin.getPinTagList().addAll(pinTags);
        }

        if (request.feedOpen() != null) {
            pin.updateFeedOpen(request.feedOpen());
        }
        return PinConverter.toUpdatedPin(pin);
    }

    @Override
    public void deletePin(Member currentMember, Long pinId) {
        Pin pin = getPin(pinId);
        validateMemberAuthorization(currentMember.getId(), pin.getMember().getId());
        pin.delete();
    }

    @Override
    public PinResponse.LikeCount createPinLike(Member currentMember, Long pinId) {
        Pin pin = getPin(pinId);
        PinLike pinLike = PinLike.create(pin, currentMember);

        try {
            pinLikeRepository.save(pinLike);
        } catch (DataIntegrityViolationException e) {
            throw new PinLikeException(PinErrorCode.ALREADY_LIKED_PIN);
        }
        pinRepository.increaseLikeCount(pinId);

        eventPublisher.publishEvent(new PinLikedEvent(pinId, pin.getMember().getId(), currentMember.getId()));

        return PinConverter.toLikeCount(pin.getLikeCount() + 1);
    }

    @Override
    public PinResponse.LikeCount deletePinLike(Member currentMember, Long pinId) {
        Pin pin = getPin(pinId);
        PinLike pinLike = pinLikeRepository.findByPinAndMember(pin, currentMember)
                        .orElseThrow(() -> new PinLikeException(PinLikeErrorCode.PIN_LIKE_NOT_FOUND));
        pinLikeRepository.delete(pinLike);
        pinRepository.decreaseLikeCount(pinId);
        return PinConverter.toLikeCount(pin.getLikeCount() - 1);
    }

    @Override
    public void increaseReportCount(Long pinId) {
        pinRepository.increaseReportCount(pinId);
    }

    private Pin getPin(Long id) {
        return pinRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new PinException(PinErrorCode.PIN_NOT_FOUND));
    }

    private void validateMemberAuthorization(Long memberId, Long writerId) {
        if (!memberId.equals(writerId)) {
            throw new PinException(PinErrorCode.INVALID_PIN_OWNER);
        }
    }
}
