package com.example.plimap.global.external.storage.supabase;

import com.example.plimap.global.external.storage.ProfileImageObjectKeyValidator;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import com.example.plimap.global.external.storage.ProfileImageStorageException;
import java.net.URI;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

public class SupabaseProfileImageStorage implements ProfileImageStorage {

    private static final String API_KEY_HEADER = "apikey";
    private static final String UPSERT_HEADER = "x-upsert";

    private final RestClient restClient;
    private final ProfileImageStorageProperties properties;

    public SupabaseProfileImageStorage(
            RestClient restClient,
            ProfileImageStorageProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public void upload(String objectKey, byte[] content, MediaType contentType) {
        ProfileImageObjectKeyValidator.validate(objectKey);
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("content must not be empty");
        }
        if (contentType == null) {
            throw new IllegalArgumentException("contentType must not be null");
        }

        try {
            restClient.post()
                    .uri(buildObjectUri(objectKey, false))
                    .header(API_KEY_HEADER, properties.supabase().secretKey())
                    .header(UPSERT_HEADER, "false")
                    .header(
                            HttpHeaders.CACHE_CONTROL,
                            Long.toString(properties.cacheControlSeconds())
                    )
                    .contentType(contentType)
                    .body(content)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new ProfileImageStorageException(
                    "프로필 이미지 업로드에 실패했습니다.",
                    exception
            );
        }
    }

    @Override
    public void delete(String objectKey) {
        ProfileImageObjectKeyValidator.validate(objectKey);

        try {
            restClient.method(HttpMethod.DELETE)
                    .uri(buildBucketUri())
                    .header(API_KEY_HEADER, properties.supabase().secretKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new DeleteObjectsRequest(List.of(objectKey)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | HttpMessageConversionException exception) {
            throw new ProfileImageStorageException(
                    "프로필 이미지 삭제에 실패했습니다.",
                    exception
            );
        }
    }

    @Override
    public URI getPublicUrl(String objectKey) {
        ProfileImageObjectKeyValidator.validate(objectKey);

        try {
            return buildObjectUri(objectKey, true);
        } catch (IllegalArgumentException exception) {
            throw new ProfileImageStorageException(
                    "프로필 이미지 공개 URL 생성에 실패했습니다.",
                    exception
            );
        }
    }

    private URI buildBucketUri() {
        return storageUriBuilder()
                .pathSegment(properties.bucket())
                .build()
                .encode()
                .toUri();
    }

    private URI buildObjectUri(String objectKey, boolean publicObject) {
        UriComponentsBuilder builder = storageUriBuilder();
        if (publicObject) {
            builder.pathSegment("public");
        }

        return builder
                .pathSegment(properties.bucket())
                .pathSegment(objectKey.split("/"))
                .build()
                .encode()
                .toUri();
    }

    private UriComponentsBuilder storageUriBuilder() {
        return UriComponentsBuilder.fromUri(properties.supabase().url())
                .pathSegment("storage", "v1", "object");
    }

    private record DeleteObjectsRequest(List<String> prefixes) {
    }
}
