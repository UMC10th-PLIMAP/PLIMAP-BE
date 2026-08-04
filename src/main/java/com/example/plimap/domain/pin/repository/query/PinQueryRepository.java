package com.example.plimap.domain.pin.repository.query;

import com.example.plimap.domain.pin.dto.Pagination;
import com.example.plimap.domain.pin.dto.PlacePinInfo;
import com.example.plimap.domain.track.dto.AlbumImage;
import com.example.plimap.domain.pin.dto.request.PinRequest;
import com.example.plimap.domain.pin.dto.response.PinResponse;
import com.example.plimap.domain.pin.entity.Pin;
import com.example.plimap.domain.pin.enums.PinSortType;
import org.locationtech.jts.geom.Point;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PinQueryRepository{
    Optional<Double> findNearestActivePinWithin10m(double latitude, double longitude);

    Map<Long, PlacePinInfo> findPinInfosByPlaceIds(List<Long> placeIds);

    Pagination<PinResponse.Feed> findFeedListByMemberId(Long memberId, String cursor, Integer pageSize, PinRequest.UserLocation request);

    Pagination<PinResponse.MyPin> findMyPinList(Long memberId, String cursor, Integer pageSize);

    Boolean existsPinByMemberFollowAndPlace(Long memberId, Long placeId);

    Pagination<PinResponse.PinDetail> findPinListByPlaceTrackIdAndSortType(Long memberId, String cursor, Integer pageSize, PinSortType pinSortType, Long placeTrackId);

    Optional<Pin> getPinPreview(Long pinId);

    boolean existsActivePinByPlaceIdAndMemberId(Long placeId, Long memberId);

    List<PinResponse.PinPreview> findPinPreviewListByViewport(Point minPoint, Point maxPoint);

    List<PinResponse.Cluster> findClusterListByViewport(Point minPoint, Point maxPoint, Integer zoomLevel);

    Pagination<PinResponse.FriendPin> getFriendRecentPinList(Long memberId, String cursor, Integer pageSize);

    Map<Long, AlbumImage> findRepresentativePlaceTracksByPlaceIds(List<Long> placeIds);
}
