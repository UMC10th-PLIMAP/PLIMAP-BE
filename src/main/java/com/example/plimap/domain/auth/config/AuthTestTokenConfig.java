package com.example.plimap.domain.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile({"local", "dev"})
@EnableConfigurationProperties(TestTokenProperties.class)
public class AuthTestTokenConfig {
}
