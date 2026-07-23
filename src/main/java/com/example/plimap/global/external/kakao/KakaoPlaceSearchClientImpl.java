package com.example.plimap.global.external.kakao;

import com.example.plimap.global.external.kakao.dto.KakaoPlaceSearchResponse;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class KakaoPlaceSearchClientImpl implements KakaoPlaceSearchClient {

    private static final String AUTHORIZATION_PREFIX = "KakaoAK ";

    private final RestClient kakaoLocalRestClient;
    private final KakaoLocalProperties properties;

    @Override
    public KakaoPlaceSearchResponse search(
            String keyword,
            double latitude,
            double longitude
    ) {
        try {
            KakaoPlaceSearchResponse response = kakaoLocalRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/keyword.json")
                            .queryParam("query", keyword)
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("sort", "distance")
                            .build())
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            AUTHORIZATION_PREFIX + properties.restApiKey()
                    )
                    .retrieve()
                    .body(KakaoPlaceSearchResponse.class);

            if (response == null) {
                throw new KakaoClientException("Kakao Local API returned an empty response");
            }
            return response;
        } catch (ResourceAccessException exception) {
            if (isTimeout(exception)) {
                throw new KakaoClientTimeoutException(
                        "Kakao Local API request timed out",
                        exception
                );
            }
            throw new KakaoClientException("Kakao Local API request failed", exception);
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new KakaoClientException("Kakao Local API request failed", exception);
        }
    }

    private boolean isTimeout(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
