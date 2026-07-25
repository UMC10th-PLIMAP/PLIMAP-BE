package com.example.plimap.global.security;

import com.example.plimap.global.config.SwaggerAccessProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

public class SwaggerHostAccessFilter extends OncePerRequestFilter {

    private static final String SWAGGER_UI_PATH = "/swagger-ui";
    private static final String SWAGGER_UI_HTML_PATH = "/swagger-ui.html";
    private static final String OPEN_API_PATH = "/v3/api-docs";
    private static final String OPEN_API_YAML_PATH = "/v3/api-docs.yaml";

    private final SwaggerAccessProperties properties;

    public SwaggerHostAccessFilter(SwaggerAccessProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !isSwaggerPath(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.allowedHost().equalsIgnoreCase(request.getServerName())) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSwaggerPath(String path) {
        return path.equals(SWAGGER_UI_PATH)
                || path.equals(SWAGGER_UI_HTML_PATH)
                || path.startsWith(SWAGGER_UI_PATH + "/")
                || path.equals(OPEN_API_PATH)
                || path.equals(OPEN_API_YAML_PATH)
                || path.startsWith(OPEN_API_PATH + "/");
    }
}
