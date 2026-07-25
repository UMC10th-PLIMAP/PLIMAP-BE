package com.example.plimap.global.security;

import com.example.plimap.global.config.OAuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OAuthFrontendRedirectCookieRepository {

    static final String PARAMETER_NAME = "frontendOrigin";
    static final String COOKIE_NAME = "oauth2_frontend_origin";
    private static final Duration COOKIE_MAX_AGE = Duration.ofMinutes(3);
    private static final String PAYLOAD_DELIMITER = "\n";

    private final AuthCookieUtil authCookieUtil;
    private final OAuthProperties oAuthProperties;

    public String validateRequestedOrigin(HttpServletRequest request) {
        String[] requestedOrigins = request.getParameterValues(PARAMETER_NAME);
        if (requestedOrigins != null && requestedOrigins.length != 1) {
            throw new IllegalArgumentException("OAuth 프론트 Origin은 하나만 전달할 수 있습니다.");
        }

        String requestedOrigin = requestedOrigins == null
                ? oAuthProperties.defaultFrontendOrigin()
                : requestedOrigins[0];
        return oAuthProperties.requireAllowedFrontendOrigin(requestedOrigin);
    }

    public void saveRequestedOrigin(HttpServletRequest request,
                                    HttpServletResponse response,
                                    String state) {
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("OAuth state는 비어 있을 수 없습니다.");
        }

        String allowedOrigin = validateRequestedOrigin(request);
        String payload = state + PAYLOAD_DELIMITER + allowedOrigin;
        String encodedPayload = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));

        authCookieUtil.setCookie(response, COOKIE_NAME, encodedPayload, COOKIE_MAX_AGE);
    }

    public String consumeRedirectUri(HttpServletRequest request, HttpServletResponse response) {
        try {
            String encodedPayload = authCookieUtil.getCookieValue(request, COOKIE_NAME);
            String callbackState = request.getParameter("state");
            if (encodedPayload == null || callbackState == null) {
                return defaultRedirectUri();
            }

            String payload = new String(
                    Base64.getUrlDecoder().decode(encodedPayload),
                    StandardCharsets.UTF_8
            );
            int delimiterIndex = payload.indexOf(PAYLOAD_DELIMITER);
            if (delimiterIndex <= 0 || delimiterIndex == payload.length() - 1) {
                return defaultRedirectUri();
            }

            String savedState = payload.substring(0, delimiterIndex);
            if (!MessageDigest.isEqual(
                    savedState.getBytes(StandardCharsets.UTF_8),
                    callbackState.getBytes(StandardCharsets.UTF_8)
            )) {
                return defaultRedirectUri();
            }

            String frontendOrigin = payload.substring(delimiterIndex + PAYLOAD_DELIMITER.length());
            return oAuthProperties.redirectUriFor(frontendOrigin);
        } catch (IllegalArgumentException exception) {
            return defaultRedirectUri();
        } finally {
            clearCookie(response);
        }
    }

    public void clearCookie(HttpServletResponse response) {
        authCookieUtil.clearCookie(response, COOKIE_NAME);
    }

    private String defaultRedirectUri() {
        return oAuthProperties.redirectUriFor(oAuthProperties.defaultFrontendOrigin());
    }
}
