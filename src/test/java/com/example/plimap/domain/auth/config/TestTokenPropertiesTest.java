package com.example.plimap.domain.auth.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TestTokenPropertiesTest {

    private static final String ISSUE_KEY = "3B7YhG0A5czm0k0xGkTxYJofXxGtDZQp1yAp3_9XgS8";

    @Test
    void Dev_발급_키가_정확히_일치하면_허용한다() {
        TestTokenProperties properties = new TestTokenProperties(true, ISSUE_KEY);

        assertThat(properties.matches(ISSUE_KEY)).isTrue();
        assertThat(properties.matches(ISSUE_KEY + "x")).isFalse();
        assertThat(properties.matches(null)).isFalse();
    }

    @Test
    void Local에서는_발급_키를_요구하지_않는다() {
        TestTokenProperties properties = new TestTokenProperties(false, null);

        assertThat(properties.matches(null)).isTrue();
        assertThat(properties.matches("아무 값")).isTrue();
    }

    @Test
    void Dev_발급_키가_비어_있거나_32바이트보다_짧으면_거부한다() {
        assertThatThrownBy(() -> new TestTokenProperties(true, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TestTokenProperties(true, "short-issue-key"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void Dev_발급_키에_앞뒤_공백이_있으면_거부한다() {
        assertThatThrownBy(() -> new TestTokenProperties(true, ISSUE_KEY + " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
