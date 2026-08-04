package com.example.plimap.global.external.storage.gcs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.plimap.global.external.storage.ProfileImageStorageException;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

class GcsProfileImageStorageTest {

    private static final String BUCKET = "plimap-prod-profile-images";
    private static final String OBJECT_KEY =
            "members/1/123e4567-e89b-12d3-a456-426614174000.webp";

    private final Storage storage = mock(Storage.class);
    private GcsProfileImageStorage profileImageStorage;

    @BeforeEach
    void setUp() {
        GcsProfileImageStorageProperties properties = new GcsProfileImageStorageProperties(
                BUCKET,
                URI.create("https://storage.googleapis.com"),
                31536000
        );
        profileImageStorage = new GcsProfileImageStorage(storage, properties);
    }

    @Test
    void 프로필_이미지를_GCS에_업로드한다() {
        byte[] content = {1, 2, 3};
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);

        profileImageStorage.upload(OBJECT_KEY, content, MediaType.IMAGE_PNG);

        verify(storage).create(
                blobInfoCaptor.capture(),
                eq(content),
                any(Storage.BlobTargetOption[].class)
        );
        BlobInfo blobInfo = blobInfoCaptor.getValue();
        assertThat(blobInfo.getBucket()).isEqualTo(BUCKET);
        assertThat(blobInfo.getName()).isEqualTo(OBJECT_KEY);
        assertThat(blobInfo.getContentType()).isEqualTo("image/png");
        assertThat(blobInfo.getCacheControl()).isEqualTo("public, max-age=31536000");
    }

    @Test
    void GCS_업로드가_실패하면_스토리지_예외로_변환한다() {
        when(storage.create(
                any(BlobInfo.class),
                any(byte[].class),
                any(Storage.BlobTargetOption[].class)
        )).thenThrow(new StorageException(500, "upload failed"));

        assertThatThrownBy(() -> profileImageStorage.upload(
                OBJECT_KEY,
                new byte[]{1},
                MediaType.IMAGE_PNG
        )).isInstanceOf(ProfileImageStorageException.class);
    }

    @Test
    void 프로필_이미지를_GCS에서_삭제한다() {
        profileImageStorage.delete(OBJECT_KEY);

        verify(storage).delete(BlobId.of(BUCKET, OBJECT_KEY));
    }

    @Test
    void GCS_삭제가_실패하면_스토리지_예외로_변환한다() {
        when(storage.delete(BlobId.of(BUCKET, OBJECT_KEY)))
                .thenThrow(new StorageException(500, "delete failed"));

        assertThatThrownBy(() -> profileImageStorage.delete(OBJECT_KEY))
                .isInstanceOf(ProfileImageStorageException.class);
    }

    @Test
    void 공개_URL에_버킷과_인코딩된_objectKey를_포함한다() {
        URI publicUrl = profileImageStorage.getPublicUrl(OBJECT_KEY);

        assertThat(publicUrl).isEqualTo(URI.create(
                "https://storage.googleapis.com/"
                        + BUCKET
                        + "/members/1/123e4567-e89b-12d3-a456-426614174000.webp"
        ));
    }

    @Test
    void 잘못된_objectKey는_GCS를_호출하기_전에_거부한다() {
        assertThatThrownBy(() -> profileImageStorage.delete("../secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}