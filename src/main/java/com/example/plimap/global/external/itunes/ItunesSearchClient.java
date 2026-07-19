package com.example.plimap.global.external.itunes;

import com.example.plimap.global.external.itunes.dto.ItunesSearchResponse;

public interface ItunesSearchClient {

    ItunesSearchResponse search(String keyword, int limit);
}
