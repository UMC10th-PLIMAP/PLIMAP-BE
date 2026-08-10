package com.example.plimap.domain.home.service.query.impl;

import com.example.plimap.domain.home.dto.response.HomeResponse;
import com.example.plimap.domain.home.service.query.HomeQueryService;
import com.example.plimap.domain.place.dto.PlaceAdministrativeRegion;
import com.example.plimap.domain.place.service.query.PlaceLocationMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeQueryServiceImpl implements HomeQueryService {

    private final PlaceLocationMetadataService placeLocationMetadataService;

    @Override
    public HomeResponse.Context getHomeContext(
            String nickname,
            double latitude,
            double longitude
    ) {
        PlaceAdministrativeRegion region =
                placeLocationMetadataService.getAdministrativeRegion(latitude, longitude);
        return new HomeResponse.Context(
                nickname,
                HomeResponse.CurrentRegion.from(region)
        );
    }
}
