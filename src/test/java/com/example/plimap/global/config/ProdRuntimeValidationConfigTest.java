package com.example.plimap.global.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProdRuntimeValidationConfigTest {

    @Test
    void Prod_Redis는_TLS_URL을_허용한다() {
        assertThatCode(() -> ProdRuntimeValidationConfig.validateRedisUrl(
                "rediss://default:encoded-password@redis.example:6380"
        )).doesNotThrowAnyException();
    }

    @Test
    void Prod_Redis는_비TLS_URL을_거부한다() {
        assertThatThrownBy(() -> ProdRuntimeValidationConfig.validateRedisUrl(
                "redis://default:encoded-password@redis.example:6379"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod Redis URL must use rediss:// with a host.");
    }

    @Test
    void Prod_Redis는_잘못된_URL을_값_노출_없이_거부한다() {
        assertThatThrownBy(() -> ProdRuntimeValidationConfig.validateRedisUrl(
                "rediss://bad%secret"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod Redis URL is invalid.");
    }

    @Test
    void Prod_DB는_PostgreSQL_JDBC_URL을_허용한다() {
        assertThatCode(() -> ProdRuntimeValidationConfig.validateDatabaseUrl(
                "jdbc:postgresql://10.20.0.3:5432/plimap_prod?sslmode=require"
        )).doesNotThrowAnyException();

        assertThatCode(() -> ProdRuntimeValidationConfig.validateDatabaseUrl(
                "jdbc:postgresql://127.0.0.1:55432/plimap_prod?sslmode=disable"
        )).doesNotThrowAnyException();
    }

    @Test
    void Prod_DB는_PostgreSQL이_아닌_URL을_거부한다() {
        assertThatThrownBy(() -> ProdRuntimeValidationConfig.validateDatabaseUrl(
                "jdbc:mysql://database.example:3306/plimap_prod"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL must use PostgreSQL JDBC.");
    }

    @Test
    void Prod_DB_URL은_자격_증명을_포함할_수_없다() {
        assertThatThrownBy(() -> ProdRuntimeValidationConfig.validateDatabaseUrl(
                "jdbc:postgresql://app:secret@10.20.0.3:5432/plimap_prod"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL must not contain credentials.");
    }

    @Test
    void Prod_DB는_Runtime과_Flyway_전용_사용자를_요구한다() {
        assertThatCode(() -> ProdRuntimeValidationConfig.validateDatabaseUsers(
                "plimap_app",
                "plimap_migrator"
        )).doesNotThrowAnyException();

        assertThatThrownBy(() -> ProdRuntimeValidationConfig.validateDatabaseUsers(
                "plimap_migrator",
                "plimap_migrator"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database runtime user is invalid.");
    }

    @Test
    void Prod_JWT_Secret은_32바이트_이상이어야_한다() {
        assertThatCode(() -> ProdRuntimeValidationConfig.validateJwtSecret(
                "0123456789abcdef0123456789abcdef"
        )).doesNotThrowAnyException();

        assertThatThrownBy(() -> ProdRuntimeValidationConfig.validateJwtSecret("too-short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod JWT secret must be at least 32 bytes.");
    }
}
