package com.example.plimap.global.external.kakao;

import com.example.plimap.global.external.kakao.dto.KakaoAddressResponse;
import com.example.plimap.global.external.kakao.dto.KakaoRegionCodeResponse;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
public class KakaoCoordinateClientImpl implements KakaoCoordinateClient {

    private static final String AUTHORIZATION_PREFIX = "KakaoAK ";

    private final RestClient kakaoLocalRestClient;
    private final KakaoLocalProperties properties;

    @Override
    public KakaoRegionCodeResponse getRegionCodes(double latitude, double longitude) {
        return execute(() -> kakaoLocalRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/geo/coord2regioncode.json")
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .retrieve()
                .body(KakaoRegionCodeResponse.class));
    }

    @Override
    public KakaoAddressResponse getAddress(double latitude, double longitude) {
        return execute(() -> kakaoLocalRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/geo/coord2address.json")
                        .queryParam("x", longitude)
                        .queryParam("y", latitude)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, authorization())
                .retrieve()
                .body(KakaoAddressResponse.class));
    }

    private <T> T execute(Supplier<T> request) {
        try {
            T response = request.get();
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

    private String authorization() {
        return AUTHORIZATION_PREFIX + properties.restApiKey();
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
