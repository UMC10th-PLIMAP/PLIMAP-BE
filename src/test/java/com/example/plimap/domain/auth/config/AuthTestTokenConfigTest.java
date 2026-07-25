package com.example.plimap.domain.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AuthTestTokenConfigTest {

    private static final String ISSUE_KEY = "3B7YhG0A5czm0k0xGkTxYJofXxGtDZQp1yAp3_9XgS8";

    @Test
    void Dev에서_발급_키가_없으면_Context_시작에_실패한다() {
        contextRunner("dev")
                .withPropertyValues("plimap.auth.test-token.issue-key-required=true")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void Dev에서_유효한_발급_키가_있으면_설정을_등록한다() {
        contextRunner("dev")
                .withPropertyValues(
                        "plimap.auth.test-token.issue-key-required=true",
                        "plimap.auth.test-token.issue-key=" + ISSUE_KEY
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(TestTokenProperties.class);
                });
    }

    @Test
    void Local에서는_발급_키_없이_설정을_등록한다() {
        contextRunner("local")
                .withPropertyValues("plimap.auth.test-token.issue-key-required=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(TestTokenProperties.class);
                });
    }

    @Test
    void Prod에서는_테스트_토큰_설정을_등록하지_않는다() {
        contextRunner("prod")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(TestTokenProperties.class);
                });
    }

    private ApplicationContextRunner contextRunner(String profile) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withUserConfiguration(AuthTestTokenConfig.class);
    }
}
