package com.example.plimap.global.config;

import com.example.plimap.global.security.SwaggerHostAccessFilter;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
@EnableConfigurationProperties(SwaggerAccessProperties.class)
public class SwaggerAccessConfig {

    @Bean
    public FilterRegistrationBean<SwaggerHostAccessFilter> swaggerHostAccessFilter(
            SwaggerAccessProperties properties
    ) {
        FilterRegistrationBean<SwaggerHostAccessFilter> registration =
                new FilterRegistrationBean<>(new SwaggerHostAccessFilter(properties));
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.addUrlPatterns(
                "/swagger-ui",
                "/swagger-ui.html",
                "/swagger-ui/*",
                "/v3/api-docs",
                "/v3/api-docs.yaml",
                "/v3/api-docs/*"
        );
        return registration;
    }
}
