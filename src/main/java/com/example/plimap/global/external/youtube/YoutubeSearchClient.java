package com.example.plimap.global.external.youtube;

import com.example.plimap.global.external.youtube.dto.YoutubeSearchResponse;

public interface YoutubeSearchClient {

    YoutubeSearchResponse search(String query, int maxResults);
}
