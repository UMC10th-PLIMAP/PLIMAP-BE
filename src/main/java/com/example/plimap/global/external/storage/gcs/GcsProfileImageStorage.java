package com.example.plimap.global.external.storage.gcs;

import com.example.plimap.global.external.storage.ProfileImageObjectKeyValidator;
import com.example.plimap.global.external.storage.ProfileImageStorage;
import com.example.plimap.global.external.storage.ProfileImageStorageException;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

public class GcsProfileImageStorage implements ProfileImageStorage {

    private final Storage storage;
    private final GcsProfileImageStorageProperties properties;

    public GcsProfileImageStorage(
            Storage storage,
            GcsProfileImageStorageProperties properties
    ) {
        this.storage = storage;
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

        BlobInfo blobInfo = BlobInfo.newBuilder(properties.bucket(), objectKey)
                .setContentType(contentType.toString())
                .setCacheControl("public, max-age=" + properties.cacheControlSeconds())
                .build();

        try {
            storage.create(blobInfo, content, Storage.BlobTargetOption.doesNotExist());
        } catch (StorageException exception) {
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
            storage.delete(BlobId.of(properties.bucket(), objectKey));
        } catch (StorageException exception) {
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
            return UriComponentsBuilder.fromUri(properties.publicBaseUrl())
                    .pathSegment(properties.bucket())
                    .pathSegment(objectKey.split("/"))
                    .build()
                    .encode()
                    .toUri();
        } catch (IllegalArgumentException exception) {
            throw new ProfileImageStorageException(
                    "프로필 이미지 공개 URL 생성에 실패했습니다.",
                    exception
            );
        }
    }
}