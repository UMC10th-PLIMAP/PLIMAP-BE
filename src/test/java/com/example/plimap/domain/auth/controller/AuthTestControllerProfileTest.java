package com.example.plimap.domain.auth.controller;

import com.example.plimap.domain.auth.service.command.TestTokenCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthTestControllerProfileTest {

    @Test
    void Local에서는_테스트_토큰_Controller를_등록한다() {
        contextRunner("local")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AuthTestController.class);
                });
    }

    @Test
    void Dev에서는_테스트_토큰_Controller를_등록한다() {
        contextRunner("dev")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(AuthTestController.class);
                });
    }

    @Test
    void Prod에서는_테스트_토큰_Controller를_등록하지_않는다() {
        contextRunner("prod")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AuthTestController.class);
                });
    }

    private ApplicationContextRunner contextRunner(String profile) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withBean(
                        TestTokenCommandService.class,
                        () -> mock(TestTokenCommandService.class)
                )
                .withUserConfiguration(AuthTestController.class);
    }
}
