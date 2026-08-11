package com.example.plimap.domain.home.service.query;

import com.example.plimap.domain.home.dto.response.HomeResponse;

public interface HomeQueryService {

    HomeResponse.Context getHomeContext(
            String nickname,
            double latitude,
            double longitude
    );
}
