package com.example.plimap.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME = "oauth2_auth_request";
    private static final Duration COOKIE_MAX_AGE = Duration.ofMinutes(3);

    private final AuthCookieUtil authCookieUtil;
    private final ObjectMapper objectMapper;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String cookieValue = authCookieUtil.getCookieValue(request, COOKIE_NAME);
        if (cookieValue == null) {
            return null;
        }
        // 클라이언트가 조작하거나 손상시킨 쿠키 값은 조용히 무시하고 인증 실패로 자연스럽게
        // 이어지도록 한다. 그대로 던지면 AuthenticationException이 아니라서 500으로 새어나간다.
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cookieValue);
            return objectMapper.readValue(bytes, CookiePayload.class).toAuthorizationRequest();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            authCookieUtil.clearCookie(response, COOKIE_NAME);
            return;
        }
        byte[] bytes = objectMapper.writeValueAsBytes(CookiePayload.from(authorizationRequest));
        String cookieValue = Base64.getUrlEncoder().encodeToString(bytes);
        authCookieUtil.setCookie(response, COOKIE_NAME, cookieValue, COOKIE_MAX_AGE);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        authCookieUtil.clearCookie(response, COOKIE_NAME);
        return authorizationRequest;
    }

    // 신뢰할 수 없는 쿠키 값을 Java 네이티브 역직렬화(SerializationUtils)로 복원하면
    // 임의 객체 역직렬화 취약점(CWE-502)으로 이어질 수 있어, 필요한 필드만 담은 DTO를 JSON으로 직렬화한다.
    private record CookiePayload(
            String authorizationUri,
            String clientId,
            String redirectUri,
            Set<String> scopes,
            String state,
            Map<String, Object> additionalParameters,
            Map<String, Object> attributes
    ) {

        static CookiePayload from(OAuth2AuthorizationRequest authorizationRequest) {
            return new CookiePayload(
                    authorizationRequest.getAuthorizationUri(),
                    authorizationRequest.getClientId(),
                    authorizationRequest.getRedirectUri(),
                    authorizationRequest.getScopes(),
                    authorizationRequest.getState(),
                    authorizationRequest.getAdditionalParameters(),
                    authorizationRequest.getAttributes()
            );
        }

        OAuth2AuthorizationRequest toAuthorizationRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes)
                    .state(state)
                    .additionalParameters(additionalParameters)
                    .attributes(attributes)
                    .build();
        }
    }
}
