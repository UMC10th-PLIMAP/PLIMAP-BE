package com.example.plimap.global.config;

import com.example.plimap.domain.member.service.command.MemberCommandService;
import com.example.plimap.global.security.MemberStatusInterceptor;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig implements WebMvcConfigurer {

    private static final String IPV4_OCTET = "(?:25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)";
    private static final Pattern PRIVATE_NETWORK_ORIGIN = Pattern.compile(
            "^(https?)://192\\.168\\." + IPV4_OCTET + "\\." + IPV4_OCTET + "(?::\\d+)?$",
            Pattern.CASE_INSENSITIVE
    );

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> allowedOrigins = corsProperties.allowedOrigins();
        Set<String> exactAllowedOrigins = allowedOrigins.stream()
                .filter(origin -> !origin.contains("*"))
                .map(origin -> origin.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        CorsConfiguration configuration = new PrivateNetworkAwareCorsConfiguration(
                allowedOrigins.stream()
                        .map(CorsProperties::privateNetworkOriginScheme)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableSet()),
                exactAllowedOrigins,
                allowedOrigins.contains(CorsProperties.PREVIEW_ORIGIN_PATTERN)
        );
        configuration.setAllowedOriginPatterns(allowedOrigins);
        configuration.setAllowedMethods(List.of(ALLOWED_METHODS));
        configuration.setAllowedHeaders(List.of(ALLOWED_HEADERS));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MemberStatusInterceptor(memberCommandService));
    }

    private static final class PrivateNetworkAwareCorsConfiguration extends CorsConfiguration {

        private final Set<String> allowedPrivateNetworkSchemes;
        private final Set<String> exactAllowedOrigins;
        private final boolean previewOriginAllowed;

        private PrivateNetworkAwareCorsConfiguration(
                Set<String> allowedPrivateNetworkSchemes,
                Set<String> exactAllowedOrigins,
                boolean previewOriginAllowed
        ) {
            this.allowedPrivateNetworkSchemes = allowedPrivateNetworkSchemes;
            this.exactAllowedOrigins = exactAllowedOrigins;
            this.previewOriginAllowed = previewOriginAllowed;
        }

        @Override
        public String checkOrigin(String requestOrigin) {
            if (requestOrigin != null) {
                String normalizedOrigin = requestOrigin.toLowerCase(Locale.ROOT);
                for (String scheme : allowedPrivateNetworkSchemes) {
                    if (normalizedOrigin.startsWith(scheme + "://192.168.")) {
                        return PRIVATE_NETWORK_ORIGIN.matcher(requestOrigin).matches()
                                ? requestOrigin
                                : null;
                    }
                }
                if (previewOriginAllowed && normalizedOrigin.startsWith("https://pr-")) {
                    if (exactAllowedOrigins.contains(normalizedOrigin)) {
                        return requestOrigin;
                    }
                    return CorsProperties.PREVIEW_ORIGIN.matcher(requestOrigin).matches()
                            ? requestOrigin
                            : null;
                }
            }
            return super.checkOrigin(requestOrigin);
        }
    }
}
