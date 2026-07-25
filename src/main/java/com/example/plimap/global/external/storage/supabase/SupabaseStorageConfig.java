package com.example.plimap.global.external.storage.supabase;

import com.example.plimap.global.external.storage.ProfileImageStorage;
import java.net.http.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Profile({"dev", "local", "test"})
@Configuration
@EnableConfigurationProperties(ProfileImageStorageProperties.class)
public class SupabaseStorageConfig {

    @Bean
    public RestClient supabaseStorageRestClient(
            ProfileImageStorageProperties properties,
            JsonMapper jsonMapper
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());

        JacksonJsonHttpMessageConverter jacksonConverter =
                new JacksonJsonHttpMessageConverter(jsonMapper);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .configureMessageConverters(converters ->
                        converters.withJsonConverter(jacksonConverter)
                )
                .build();
    }

    @Bean
    public ProfileImageStorage profileImageStorage(
            @Qualifier("supabaseStorageRestClient")
            RestClient supabaseStorageRestClient,
            ProfileImageStorageProperties properties
    ) {
        if (properties.provider() != ProfileImageStorageProperties.Provider.SUPABASE) {
            throw new IllegalStateException(
                    "The dev/local/test profiles require the SUPABASE profile image storage provider."
            );
        }

        return new SupabaseProfileImageStorage(supabaseStorageRestClient, properties);
    }
}
