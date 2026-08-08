package com.example.plimap.domain.track.service.command;

import com.example.plimap.global.external.itunes.ItunesProperties;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class AlbumImageUrlResolver {

    private static final Pattern ARTWORK_FILE_PATTERN = Pattern.compile(
            "^100x100(?=(?:bb)?\\.(?:jpg|jpeg|png|webp)$)",
            Pattern.CASE_INSENSITIVE
    );

    private final RestClient restClient;

    @Autowired
    public AlbumImageUrlResolver(ItunesProperties properties) {
        this(createRestClient(properties));
    }

    AlbumImageUrlResolver(RestClient restClient) {
        this.restClient = restClient;
    }

    public String resolve(String artworkUrl100) {
        if (artworkUrl100 == null) {
            return null;
        }

        String highResolutionUrl = toHighResolutionUrl(artworkUrl100);
        if (highResolutionUrl.equals(artworkUrl100)) {
            return artworkUrl100;
        }

        try {
            URI highResolutionUri = URI.create(highResolutionUrl);
            if (!isAllowedArtworkUri(highResolutionUri)) {
                return artworkUrl100;
            }

            boolean available = restClient.get()
                    .uri(highResolutionUri)
                    .header(HttpHeaders.RANGE, "bytes=0-0")
                    .exchange((request, response) ->
                            response.getStatusCode().is2xxSuccessful());
            return available ? highResolutionUrl : artworkUrl100;
        } catch (Exception exception) {
            log.debug(
                    "고해상도 앨범 이미지 확인에 실패하여 원본 URL을 사용합니다.",
                    exception
            );
            return artworkUrl100;
        }
    }

    private boolean isAllowedArtworkUri(URI uri) {
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null) {
            return false;
        }

        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return normalizedHost.equals("mzstatic.com")
                || normalizedHost.endsWith(".mzstatic.com");
    }

    private String toHighResolutionUrl(String artworkUrl100) {
        int pathEnd = findPathEnd(artworkUrl100);
        int segmentStart = artworkUrl100.lastIndexOf('/', pathEnd - 1) + 1;
        String lastPathSegment = artworkUrl100.substring(segmentStart, pathEnd);

        if (!ARTWORK_FILE_PATTERN.matcher(lastPathSegment).find()) {
            return artworkUrl100;
        }

        return artworkUrl100.substring(0, segmentStart)
                + "600x600"
                + lastPathSegment.substring("100x100".length())
                + artworkUrl100.substring(pathEnd);
    }

    private int findPathEnd(String url) {
        int queryStart = url.indexOf('?');
        int fragmentStart = url.indexOf('#');

        if (queryStart < 0) {
            return fragmentStart < 0 ? url.length() : fragmentStart;
        }
        if (fragmentStart < 0) {
            return queryStart;
        }
        return Math.min(queryStart, fragmentStart);
    }

    private static RestClient createRestClient(ItunesProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
