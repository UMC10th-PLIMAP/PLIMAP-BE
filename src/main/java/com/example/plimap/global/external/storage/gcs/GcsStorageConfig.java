package com.example.plimap.global.external.storage.gcs;

import com.example.plimap.global.external.storage.ProfileImageStorage;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("prod")
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GcsProfileImageStorageProperties.class)
public class GcsStorageConfig {

    @Bean
    public Storage gcsStorage() {
        return StorageOptions.getDefaultInstance().getService();
    }

    @Bean
    public ProfileImageStorage profileImageStorage(
            Storage gcsStorage,
            GcsProfileImageStorageProperties properties
    ) {
        return new GcsProfileImageStorage(gcsStorage, properties);
    }
}