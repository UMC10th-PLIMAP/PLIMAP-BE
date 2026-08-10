package com.example.plimap.domain.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class MemberOpenApiIntegrationTest {

    private static final String MEMBER_SEARCH_PATH = "/api/v1/members/search";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 회원_검색_OpenAPI는_MemberSearchItem_스키마와_응답_계약을_노출한다() throws Exception {
        JsonNode openApi = fetchOpenApi();

        JsonNode operation = openApi.path("paths").path(MEMBER_SEARCH_PATH).path("get");
        assertThat(operation.path("responses").has("200")).isTrue();

        JsonNode pageSize = findParameter(operation, "pageSize");
        assertThat(pageSize.path("schema").path("minimum").asInt()).isEqualTo(1);
        assertThat(pageSize.path("schema").path("maximum").asInt()).isEqualTo(50);
        assertThat(pageSize.path("schema").path("default").asInt()).isEqualTo(10);

        JsonNode responseSchema = operation.path("responses").path("200")
                .path("content").path("*/*").path("schema");
        JsonNode paginationSchema = resolveReferencedSchema(openApi, responseSchema)
                .path("properties").path("result");
        JsonNode itemSchema = resolveReferencedSchema(openApi, paginationSchema)
                .path("properties").path("data").path("items");

        JsonNode itemProperties = resolveReferencedSchema(openApi, itemSchema).path("properties");
        assertThat(itemProperties.has("id")).isTrue();
        assertThat(itemProperties.has("nickname")).isTrue();
        assertThat(itemProperties.has("name")).isTrue();
        assertThat(itemProperties.has("profileImageUrl")).isTrue();
        assertThat(itemProperties.has("isFollowing")).isTrue();
        assertThat(itemProperties.has("isFollowingViewer")).isTrue();
        assertThat(itemProperties.has("joinedAt")).isTrue();
    }

    @Test
    void 회원_검색_응답_스키마는_장소_검색_SearchItem_스키마와_이름이_충돌하지_않는다() throws Exception {
        JsonNode openApi = fetchOpenApi();

        JsonNode schemas = openApi.path("components").path("schemas");
        assertThat(schemas.has("MemberSearchItem")).isTrue();

        JsonNode memberSearchItemProperties = schemas.path("MemberSearchItem").path("properties");
        assertThat(memberSearchItemProperties.has("resultType")).isFalse();
        assertThat(memberSearchItemProperties.has("placeName")).isFalse();

        JsonNode placeSearchItem = findPlaceSearchItemSchema(openApi);
        assertThat(placeSearchItem.path("properties").has("isFollowing")).isFalse();
        assertThat(placeSearchItem.path("properties").has("joinedAt")).isFalse();
    }

    private JsonNode fetchOpenApi() throws Exception {
        String responseBody = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(responseBody);
    }

    private JsonNode findParameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("OpenAPI parameter not found: " + name);
    }

    private JsonNode findPlaceSearchItemSchema(JsonNode openApi) {
        for (var entry : openApi.path("components").path("schemas").properties()) {
            JsonNode properties = entry.getValue().path("properties");
            if (properties.has("resultType")
                    && properties.has("providerPlaceId")
                    && properties.has("distanceMeters")
                    && properties.has("firstPinCreatorNickname")) {
                return entry.getValue();
            }
        }
        throw new AssertionError("Place search item schema not found");
    }

    private JsonNode resolveReferencedSchema(JsonNode openApi, JsonNode propertySchema) {
        String reference = propertySchema.path("$ref").asText();
        int separatorIndex = reference.lastIndexOf('/');
        if (separatorIndex < 0 || separatorIndex == reference.length() - 1) {
            throw new AssertionError("Referenced schema not found: " + propertySchema);
        }
        return openApi
                .path("components")
                .path("schemas")
                .path(reference.substring(separatorIndex + 1));
    }
}
