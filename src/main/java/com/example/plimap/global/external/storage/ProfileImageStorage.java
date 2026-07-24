package com.example.plimap.global.external.storage;

import java.net.URI;
import org.springframework.http.MediaType;

public interface ProfileImageStorage {

    void upload(String objectKey, byte[] content, MediaType contentType);

    void delete(String objectKey);

    URI getPublicUrl(String objectKey);
}
