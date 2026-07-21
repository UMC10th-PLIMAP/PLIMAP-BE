package com.example.plimap.domain.pin.converter;

import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.track.entity.Track;

public class PinConverter {

    public static PinResponse.Summary toSummary(
            Member member,
            Pin pin,
            Track track,
            Long placeId
    ) {
        return PinResponse.Summary.builder()
                .pinId(pin.getId())
                .placeId(placeId)
                .writerNickname(member.getNickname())
                .writerProfileImage(member.getProfileImageObjectKey())
                .introduction(pin.getIntroduction())
                .clipStartMs(pin.getClipStartMs())
                .previewUrl(track.getPreviewUrl())
                .build();
    }
}
