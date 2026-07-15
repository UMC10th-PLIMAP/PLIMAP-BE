package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.entity.AuthMember;
import com.example.plimap.domain.member.entity.Member;
import com.example.plimap.domain.member.repository.MemberRepository;
import com.example.plimap.global.security.JwtUtil;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
