package com.example.plimap.global.config;

import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.global.security.MemberStatusInterceptor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig implements WebMvcConfigurer {

    private static final String[] ALLOWED_METHODS = {
        "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    };
    private static final String[] ALLOWED_HEADERS = {
        "Authorization", "Content-Type", "X-XSRF-TOKEN"
    };

    private final CorsProperties corsProperties;
    private final MemberCommandService memberCommandService;

    public CorsConfig(CorsProperties corsProperties, MemberCommandService memberCommandService) {
        this.corsProperties = corsProperties;
        this.memberCommandService = memberCommandService;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(ALLOWED_HEADERS)
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MemberStatusInterceptor(memberCommandService));
    }
}
