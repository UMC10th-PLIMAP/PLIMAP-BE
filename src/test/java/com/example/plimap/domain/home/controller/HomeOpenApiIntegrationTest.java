package com.example.plimap.domain.home.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostgisContainerConfiguration.class, RedisContainerConfiguration.class})
class HomeOpenApiIntegrationTest {

    private static final String HOME_CONTEXT_PATH = "/api/v1/home/context";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 홈_컨텍스트_OpenAPI는_요청_응답과_오류_계약을_노출한다() throws Exception {
        String responseBody = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode openApi = objectMapper.readTree(responseBody);

        JsonNode path = openApi.path("paths").path(HOME_CONTEXT_PATH);
        JsonNode operation = path.path("get");
        assertThat(path.has("get")).isTrue();
        assertThat(path.has("post")).isFalse();
        assertThat(path.has("put")).isFalse();
        assertThat(path.has("patch")).isFalse();
        assertThat(path.has("delete")).isFalse();
        assertThat(operation.isMissingNode()).isFalse();
        assertThat(operation.has("requestBody")).isFalse();
        assertThat(operation.path("tags").get(0).asText()).isEqualTo("Home");
        assertThat(operation.path("description").asText())
                .contains("행정동 결과가 없으면 지역 필드를 null")
                .contains("위치 권한 거부")
                .contains("API 오류가 아닙니다");
        assertThat(openApi.path("security").get(0).has("JWT TOKEN")).isTrue();
        assertThat(operation.has("security")).isFalse();
        assertThat(operation.path("responses").has("200")).isTrue();
        assertThat(operation.path("responses").has("400")).isTrue();
        assertThat(operation.path("responses").has("401")).isTrue();
        assertThat(operation.path("responses").has("502")).isTrue();
        assertThat(operation.path("responses").has("504")).isTrue();

        JsonNode latitude = findParameter(operation, "latitude");
        JsonNode longitude = findParameter(operation, "longitude");
        assertThat(latitude.path("required").asBoolean()).isTrue();
        assertThat(longitude.path("required").asBoolean()).isTrue();
        assertThat(latitude.path("schema").path("minimum").asDouble()).isEqualTo(-90.0);
        assertThat(latitude.path("schema").path("maximum").asDouble()).isEqualTo(90.0);
        assertThat(longitude.path("schema").path("minimum").asDouble()).isEqualTo(-180.0);
        assertThat(longitude.path("schema").path("maximum").asDouble()).isEqualTo(180.0);

        JsonNode contextSchema = openApi.path("components").path("schemas")
                .path("HomeContextResponse");
        JsonNode contextProperties = contextSchema.path("properties");
        assertThat(contextProperties.has("nickname")).isTrue();
        assertThat(isNullableSchema(contextProperties.path("nickname"))).isTrue();
        assertThat(contextProperties.has("currentRegion")).isTrue();
        assertThat(requiredProperties(contextSchema)).contains("currentRegion");
        assertThat(isNullableSchema(contextProperties.path("currentRegion"))).isFalse();

        JsonNode regionProperties = openApi.path("components").path("schemas")
                .path("HomeCurrentRegionResponse").path("properties");
        assertThat(regionProperties.has("sido")).isTrue();
        assertThat(regionProperties.has("sigungu")).isTrue();
        assertThat(regionProperties.has("eupMyeonDong")).isTrue();
        assertThat(regionProperties.has("displayName")).isTrue();
        assertThat(isNullableSchema(regionProperties.path("sido"))).isTrue();
        assertThat(isNullableSchema(regionProperties.path("sigungu"))).isTrue();
        assertThat(isNullableSchema(regionProperties.path("eupMyeonDong"))).isTrue();
        assertThat(isNullableSchema(regionProperties.path("displayName"))).isTrue();
    }

    private JsonNode findParameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("OpenAPI parameter not found: " + name);
    }

    private List<String> requiredProperties(JsonNode schema) {
        List<String> values = new ArrayList<>();
        schema.path("required").forEach(value -> values.add(value.asText()));
        return values;
    }

    private boolean isNullableSchema(JsonNode schema) {
        if (schema.path("nullable").asBoolean()
                || "null".equals(schema.path("type").asText())) {
            return true;
        }
        for (JsonNode type : schema.path("type")) {
            if ("null".equals(type.asText())) {
                return true;
            }
        }
        return false;
    }
}
