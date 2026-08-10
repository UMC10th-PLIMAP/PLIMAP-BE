package com.example.plimap.domain.pin.repository.query;

import com.example.plimap.domain.pin.dto.response.PinResponse;
import org.locationtech.jts.geom.Point;

import java.util.List;

public interface ClusterAndPinRepository {
    List<PinResponse.PinPreview> findPinPreviewListByPlaceIds(List<Long> placeIds);

    List<PinResponse.PinPreview> findPinPreviewListByViewport(Point minPoint, Point maxPoint);

    List<PinResponse.Cluster> findClusterListByViewport(Point minPoint, Point maxPoint, Integer zoomLevel, Long memberId);

    PinResponse.ClusterAndPin findGeohashClusterListByViewport(Point minPoint, Point maxPoint, Integer zoomLevel, Integer precision, Long memberId);

}
