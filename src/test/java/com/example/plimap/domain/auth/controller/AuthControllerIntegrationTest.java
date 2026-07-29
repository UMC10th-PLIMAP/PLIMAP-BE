package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void 로컬_Origin에서_발급받은_CSRF_토큰으로_쿠키_인증_POST에_성공한다() throws Exception {
        String localOrigin = "http://localhost:5173";
        MvcResult csrfResult = mockMvc.perform(get("/api/v1/auth/csrf")
                        .header(HttpHeaders.ORIGIN, localOrigin))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, localOrigin))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(jsonPath("$.code").value("AUTH_200_CSRF_TOKEN_ISSUED"))
                .andExpect(jsonPath("$.result.token").isNotEmpty())
                .andReturn();

        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        String csrfToken = JsonPath.read(
                csrfResult.getResponse().getContentAsString(),
                "$.result.token"
        );
        String accessToken = issueAccessToken();

        mockMvc.perform(post("/api/v1/auth/terms")
                        .header(HttpHeaders.ORIGIN, localOrigin)
                        .header("X-XSRF-TOKEN", csrfToken)
                        .cookie(new Cookie("accessToken", accessToken), csrfCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreements": [
                                    { "type": "SERVICE", "agreed": true },
                                    { "type": "PRIVACY", "agreed": true },
                                    { "type": "LOCATION", "agreed": true }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, localOrigin))
                .andExpect(jsonPath("$.code").value("TERMS_200_TERMS_AGREED"));
    }

    @Test
    void 필수_약관_중_일부만_동의하면_약관_동의_필수_에러를_반환한다() throws Exception {
        String accessToken = issueAccessToken();

        mockMvc.perform(post("/api/v1/auth/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreements": [
                                    {
                                      "type": "SERVICE",
                                      "agreed": true
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("TERMS_400_AGREEMENT_REQUIRED"));
    }

    @Test
    void 활성_필수_약관에_모두_동의하면_약관_동의에_성공한다() throws Exception {
        String accessToken = issueAccessToken();

        mockMvc.perform(post("/api/v1/auth/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreements": [
                                    { "type": "SERVICE", "agreed": true },
                                    { "type": "PRIVACY", "agreed": true },
                                    { "type": "LOCATION", "agreed": true }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("TERMS_200_TERMS_AGREED"))
                .andExpect(jsonPath("$.result[?(@.type == 'SERVICE')].agreed").value(true));
    }

    @Test
    void 약관_동의_여부를_조회하면_동의한_약관은_true_동의하지_않은_약관은_false로_응답한다() throws Exception {
        // given
        String accessToken = issueAccessToken();
        mockMvc.perform(post("/api/v1/auth/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreements": [
                                    { "type": "SERVICE", "agreed": true },
                                    { "type": "PRIVACY", "agreed": true },
                                    { "type": "LOCATION", "agreed": true }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        // when
        var result = mockMvc.perform(get("/api/v1/auth/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("TERMS_200_AGREEMENT_STATUS_RETRIEVED"))
                .andExpect(jsonPath("$.result[*].type").value(contains("LOCATION", "MARKETING", "PRIVACY", "SERVICE")))
                .andExpect(jsonPath("$.result[?(@.type == 'SERVICE')].agreed").value(true))
                .andExpect(jsonPath("$.result[?(@.type == 'SERVICE')].agreedAt").exists())
                .andExpect(jsonPath("$.result[?(@.type == 'MARKETING')].agreed").value(false))
                .andExpect(jsonPath("$.result[1].agreedAt").value(nullValue()));
    }

    @Test
    void 약관_동의_여부_조회는_다른_회원의_동의_이력을_노출하지_않는다() throws Exception {
        // given
        String agreedMemberToken = issueAccessToken();
        mockMvc.perform(post("/api/v1/auth/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agreedMemberToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agreements": [
                                    { "type": "SERVICE", "agreed": true },
                                    { "type": "PRIVACY", "agreed": true },
                                    { "type": "LOCATION", "agreed": true }
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        String otherMemberToken = issueAccessToken();

        // when
        var result = mockMvc.perform(get("/api/v1/auth/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherMemberToken));

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.result[?(@.agreed == true)]").isEmpty());
    }

    @Test
    void 로그아웃하면_쿠키가_삭제되고_해당_토큰은_이후_요청에서_거부된다() throws Exception {
        String accessToken = issueAccessToken();

        var result = mockMvc.perform(delete("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MEMBER_200_LOGOUT"))
                .andReturn();

        var accessTokenCookie = result.getResponse().getCookie("accessToken");
        assertThat(accessTokenCookie).isNotNull();
        assertThat(accessTokenCookie.getMaxAge()).isZero();

        mockMvc.perform(get("/api/v1/auth/terms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("COMMON_401_UNAUTHORIZED"));
    }

    private String issueAccessToken() {
        Member member = memberRepository.saveAndFlush(Member.builder().build());
        return jwtUtil.createAccessToken(new AuthMember(member));
    }
}
