package com.example.plimap.domain.pin.service.command.impl;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.converter.PinConverter;
import com.example.plimap.domain.pin.dto.PlaceAccessToken;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.entity.PinTag;
import com.example.plimap.domain.pin.entity.Tag;
import com.example.plimap.domain.pin.event.PinCreatedEvent;
import com.example.plimap.domain.pin.event.PinLikedEvent;
import com.example.plimap.domain.pin.exception.*;
import com.example.plimap.domain.pin.repository.PinLikeRepository;
import com.example.plimap.domain.pin.repository.PinRepository;
import com.example.plimap.domain.pin.repository.PinTagRepository;
import com.example.plimap.domain.pin.repository.PlaceAccessTokenRepository;
import com.example.plimap.domain.pin.repository.query.PinQueryRepository;
import com.example.plimap.domain.pin.service.command.PinCommandService;
import com.example.plimap.domain.pin.service.query.TagQueryService;
import com.example.plimap.domain.pin.validator.PinLocationValidator;
import com.example.plimap.domain.place.entity.Place;
import com.example.plimap.domain.place.service.query.PlaceQueryService;
import com.example.plimap.domain.track.dto.request.TrackCommand;
import com.example.plimap.domain.track.entity.PlaceTrack;
import com.example.plimap.domain.track.service.command.TrackCommandService;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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
    private final ProfileImageStorage profileImageStorage;
    private final PlaceAccessTokenRepository placeAccessTokenRepository;
    private final PinQueryRepository pinQueryRepository;

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

        String profileImageUrl = profileImageStorage.getPublicUrlOrNull(currentMember.getProfileImageObjectKey());

        return PinConverter.toSummary(currentMember, pin, place.getId(), profileImageUrl);
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
        int inserted = pinLikeRepository.insertIfAbsent(
                pinId,
                currentMember.getId()
        );

        if (inserted == 1) {
            pinRepository.increaseLikeCount(pinId);

            eventPublisher.publishEvent(
                    new PinLikedEvent(
                            pinId,
                            pin.getMember().getId(),
                            currentMember.getId()
                    )
            );
        }

        Integer likeCount = pinRepository.findLikeCountById(pinId);

        return PinConverter.toLikeCount(likeCount, true);
    }

    @Override
    public PinResponse.LikeCount deletePinLike(Member currentMember, Long pinId) {
        int deleted = pinLikeRepository.deleteByPinIdAndMemberId(
                pinId,
                currentMember.getId()
        );

        if (deleted == 1) {
            pinRepository.decreaseLikeCount(pinId);
        }

        Integer likeCount = pinRepository.findLikeCountById(pinId);

        return PinConverter.toLikeCount(likeCount, false);
    }

    @Override
    public void increaseReportCount(Long pinId) {
        pinRepository.increaseReportCount(pinId);
    }

    @Override
    public void decreaseReportCount(Long pinId) {
        pinRepository.decreaseReportCount(pinId);
    }

    @Override
    public void decreaseLikeCount(Long pinId) {
        pinRepository.decreaseLikeCount(pinId);
    }

    @Override
    public void penalizePin(Long pinId) {
        Pin pin = getPin(pinId);
        pin.penalize();
    }

    @Override
    public void resetPinReportCount(Long pinId) {
        Pin pin = getPin(pinId);
        pin.resetReportCount();
    }

    @Override
    public void hardDeleteAllByMember(Long memberId) {
        pinRepository.deleteByMemberId(memberId);
    }

    @Override
    public void hardDeleteLikesByMember(Long memberId) {
        pinLikeRepository.deleteByMemberId(memberId);
    }

    @Override
    public PinResponse.PlaceAccessToken createPlaceAccessToken(Long memberId, Long placeId) {
        if (!pinQueryRepository.existsPinByMemberFollowAndPlace(memberId, placeId)) {
            throw new PinException(PinErrorCode.FRIEND_PIN_ACCESS_DENIED);
        }

        String token = UUID.randomUUID().toString();

        PlaceAccessToken accessToken = PlaceAccessToken.builder()
                .memberId(memberId)
                .placeId(placeId)
                .build();

        placeAccessTokenRepository.save(accessToken, token);
        return PinConverter.toPlaceAccessToken(placeId, token);
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
