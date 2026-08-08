package com.example.plimap.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;
import org.junit.jupiter.api.Test;

class ProdRuntimeValidationConfigTest {

    @Test
    void Prod_Redis는_TLS_URL을_허용한다() {
        // given
        String redisUrl = "rediss://default:encoded-password@redis.example:6380";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateRedisUrl(redisUrl)
        );

        // then
        assertThat(thrown).isNull();
    }

    @Test
    void Prod_Redis는_비TLS_URL을_거부한다() {
        // given
        String redisUrl = "redis://default:encoded-password@redis.example:6379";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateRedisUrl(redisUrl)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod Redis URL must use rediss:// with a host.");
    }

    @Test
    void Prod_Redis는_잘못된_URL을_값_노출_없이_거부한다() {
        // given
        String redisUrl = "rediss://bad%secret";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateRedisUrl(redisUrl)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod Redis URL is invalid.");
    }

    @Test
    void Prod_DB는_Cloud_SQL_TLS_JDBC_URL을_허용한다() {
        // given
        String databaseUrl =
                "jdbc:postgresql://10.20.0.3:5432/plimap_prod?sslmode=require";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUrl(databaseUrl)
        );

        // then
        assertThat(thrown).isNull();
    }

    @Test
    void Prod_DB는_모든_loopback_URL을_거부한다() {
        // given
        List<String> databaseUrls = List.of(
                "jdbc:postgresql://localhost:5432/plimap_prod?sslmode=require",
                "jdbc:postgresql://127.0.0.1:5432/plimap_prod?sslmode=require",
                "jdbc:postgresql://[::1]:5432/plimap_prod?sslmode=require"
        );

        // when
        List<Throwable> thrown = databaseUrls.stream()
                .map(databaseUrl -> catchThrowable(
                        () -> ProdRuntimeValidationConfig.validateDatabaseUrl(databaseUrl)
                ))
                .toList();

        // then
        assertThat(thrown).allSatisfy(exception -> assertThat(exception)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL is invalid."));
    }

    @Test
    void Prod_DB는_비표준_Port를_거부한다() {
        // given
        String databaseUrl =
                "jdbc:postgresql://10.20.0.3:55432/plimap_prod?sslmode=require";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUrl(databaseUrl)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL is invalid.");
    }

    @Test
    void Prod_DB는_sslmode가_없으면_거부한다() {
        // given
        String databaseUrl = "jdbc:postgresql://10.20.0.3:5432/plimap_prod";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUrl(databaseUrl)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL must use sslmode=require.");
    }

    @Test
    void Prod_DB는_비TLS_sslmode를_거부한다() {
        // given
        String databaseUrl =
                "jdbc:postgresql://10.20.0.3:5432/plimap_prod?sslmode=disable";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUrl(databaseUrl)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL must use sslmode=require.");
    }

    @Test
    void Prod_DB는_중복_sslmode를_거부한다() {
        // given
        String databaseUrl =
                "jdbc:postgresql://10.20.0.3:5432/plimap_prod"
                        + "?sslmode=require&sslmode=disable";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUrl(databaseUrl)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL must use sslmode=require.");
    }

    @Test
    void Prod_DB는_PostgreSQL이_아닌_URL을_거부한다() {
        // given
        String databaseUrl = "jdbc:mysql://database.example:3306/plimap_prod";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUrl(databaseUrl)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL must use PostgreSQL JDBC.");
    }

    @Test
    void Prod_DB_URL은_UserInfo_자격_증명을_포함할_수_없다() {
        // given
        String databaseUrl =
                "jdbc:postgresql://app:secret@10.20.0.3:5432/plimap_prod?sslmode=require";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUrl(databaseUrl)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL must not contain credentials.");
    }

    @Test
    void Prod_DB_URL은_인코딩된_Query_자격_증명을_포함할_수_없다() {
        // given
        String databaseUrl =
                "jdbc:postgresql://10.20.0.3:5432/plimap_prod"
                        + "?sslmode=require&%75ser=plimap_app";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUrl(databaseUrl)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database URL must not contain credentials.");
    }

    @Test
    void Prod_DB는_Runtime과_Flyway_전용_사용자를_허용한다() {
        // given
        String databaseUsername = "plimap_app";
        String flywayUsername = "plimap_migrator";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUsers(
                        databaseUsername,
                        flywayUsername
                )
        );

        // then
        assertThat(thrown).isNull();
    }

    @Test
    void Prod_DB는_잘못된_Runtime_사용자를_거부한다() {
        // given
        String databaseUsername = "plimap_migrator";
        String flywayUsername = "plimap_migrator";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUsers(
                        databaseUsername,
                        flywayUsername
                )
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod database runtime user is invalid.");
    }

    @Test
    void Prod_DB는_잘못된_Flyway_사용자를_거부한다() {
        // given
        String databaseUsername = "plimap_app";
        String flywayUsername = "plimap_app";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateDatabaseUsers(
                        databaseUsername,
                        flywayUsername
                )
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod Flyway user is invalid.");
    }

    @Test
    void Prod_JWT_Secret은_32바이트_이상이면_허용한다() {
        // given
        String jwtSecret = "0123456789abcdef0123456789abcdef";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateJwtSecret(jwtSecret)
        );

        // then
        assertThat(thrown).isNull();
    }

    @Test
    void Prod_JWT_Secret은_32바이트_미만이면_거부한다() {
        // given
        String jwtSecret = "too-short";

        // when
        Throwable thrown = catchThrowable(
                () -> ProdRuntimeValidationConfig.validateJwtSecret(jwtSecret)
        );

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Prod JWT secret must be at least 32 bytes.");
    }
}
