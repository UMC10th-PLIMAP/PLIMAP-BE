package com.example.plimap.global.external.itunes;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableConfigurationProperties(ItunesProperties.class)
public class ItunesClientConfig {

    @Bean
    public RestClient itunesRestClient(
            ItunesProperties properties,
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

        List<MediaType> supportedMediaTypes =
                new ArrayList<>(jacksonConverter.getSupportedMediaTypes());

        supportedMediaTypes.add(
                MediaType.parseMediaType("text/javascript;charset=UTF-8")
        );

        jacksonConverter.setSupportedMediaTypes(supportedMediaTypes);

        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .configureMessageConverters(converters ->
                        converters.withJsonConverter(jacksonConverter)
                )
                .build();
    }
}
