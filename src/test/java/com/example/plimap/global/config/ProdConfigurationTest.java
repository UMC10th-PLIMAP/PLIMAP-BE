package com.example.plimap.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

class ProdConfigurationTest {

    @Test
    void Prod_HikariCP와_GCS_Swagger_기본값을_고정한다() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(
                "application-prod",
                new ClassPathResource("application-prod.yml")
        );
        PropertySource<?> properties = propertySources.get(0);

        assertThat(properties.getProperty("spring.datasource.hikari.maximum-pool-size"))
                .isEqualTo(8);
        assertThat(properties.getProperty("spring.datasource.hikari.minimum-idle"))
                .isEqualTo(0);
        assertThat(properties.getProperty("spring.datasource.hikari.connection-timeout"))
                .isEqualTo(5000);
        assertThat(properties.getProperty("profile-image.storage.gcs.bucket"))
                .isEqualTo("${PROFILE_IMAGE_BUCKET}");
        assertThat(properties.getProperty("springdoc.api-docs.enabled"))
                .isEqualTo("${SWAGGER_ENABLED:false}");
        assertThat(properties.getProperty("springdoc.swagger-ui.enabled"))
                .isEqualTo("${SWAGGER_ENABLED:false}");
    }
}