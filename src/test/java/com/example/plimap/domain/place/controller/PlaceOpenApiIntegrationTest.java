package com.example.plimap.domain.place.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
class PlaceOpenApiIntegrationTest {

    private static final String PLACE_SEARCH_PATH = "/api/v1/places/search";
    private static final String PLACE_SELECTION_PATH = "/api/v1/places/selections";
    private static final String PLACE_DETAIL_PATH = "/api/v1/places/{placeId}";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 장소_상세_OpenAPI는_최신_요청과_응답_계약을_노출한다() throws Exception {
        String responseBody = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode openApi = objectMapper.readTree(responseBody);

        JsonNode operation = openApi
                .path("paths")
                .path(PLACE_DETAIL_PATH)
                .path("get");
        assertThat(operation.path("description").asText())
                .contains("isTrackDetailAccessible");
        assertThat(operation.path("responses").has("200")).isTrue();
        assertThat(operation.path("responses").has("400")).isTrue();
        assertThat(operation.path("responses").has("401")).isTrue();
        assertThat(operation.path("responses").has("404")).isTrue();

        JsonNode latitude = findParameter(operation, "latitude");
        JsonNode longitude = findParameter(operation, "longitude");
        assertThat(latitude.path("required").asBoolean()).isTrue();
        assertThat(longitude.path("required").asBoolean()).isTrue();
        assertThat(latitude.path("schema").path("minimum").asDouble()).isEqualTo(-90.0);
        assertThat(latitude.path("schema").path("maximum").asDouble()).isEqualTo(90.0);
        assertThat(longitude.path("schema").path("minimum").asDouble()).isEqualTo(-180.0);
        assertThat(longitude.path("schema").path("maximum").asDouble()).isEqualTo(180.0);

        JsonNode properties = openApi
                .path("components")
                .path("schemas")
                .path("PlaceDetailResponse")
                .path("properties");
        assertThat(properties.has("placeId")).isTrue();
        assertThat(properties.has("placeName")).isTrue();
        assertThat(properties.has("category")).isTrue();
        assertThat(properties.has("address")).isTrue();
        assertThat(properties.has("roadAddress")).isTrue();
        assertThat(properties.has("latitude")).isTrue();
        assertThat(properties.has("longitude")).isTrue();
        assertThat(properties.has("distanceMeters")).isTrue();
        assertThat(properties.has("withinAccessRange")).isTrue();
        assertThat(properties.has("hasPin")).isTrue();
        assertThat(properties.has("pinCount")).isTrue();
        assertThat(properties.has("bookmarkedByMe")).isTrue();
        assertThat(properties.has("pinnedByMe")).isTrue();
        assertThat(properties.has("detailAccessible")).isFalse();
        assertThat(properties.has("likedTrackAtPlaceByMe")).isFalse();
        assertThat(properties.has("followedMemberPinnedAtPlace")).isFalse();
    }

    @Test
    void 장소_검색_OpenAPI는_PLACE와_ADDRESS_응답_계약을_노출한다() throws Exception {
        String responseBody = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode openApi = objectMapper.readTree(responseBody);

        String description = openApi
                .path("paths")
                .path(PLACE_SEARCH_PATH)
                .path("get")
                .path("description")
                .asText();
        assertThat(description).contains("주소를 먼저 검색");

        JsonNode searchItemSchema = findSearchItemSchema(openApi);
        JsonNode properties = searchItemSchema.path("properties");
        assertThat(enumValues(properties.path("resultType")))
                .containsExactlyInAnyOrder("PLACE", "ADDRESS");
        assertThat(properties.path("providerPlaceId").path("description").asText())
                .contains("ADDRESS 결과는 null");
        assertThat(properties.path("category").path("description").asText())
                .contains("ADDRESS 결과는 null");
        assertThat(properties.path("placeName").path("description").asText())
                .contains("roadAddress, address 순");
        assertThat(properties.path("roadAddress").path("description").asText())
                .contains("없는 경우 null");
    }

    @Test
    void 장소_선택_OpenAPI는_ADDRESS_요청과_응답_계약을_노출한다() throws Exception {
        String responseBody = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode openApi = objectMapper.readTree(responseBody);

        JsonNode operation = openApi
                .path("paths")
                .path(PLACE_SELECTION_PATH)
                .path("post");
        assertThat(operation.path("description").asText())
                .contains("ADDRESS_SEARCH")
                .contains("전체 지번 주소 기준");

        JsonNode selectionSchema = findSelectionRequestSchema(openApi);
        JsonNode properties = selectionSchema.path("properties");
        assertThat(enumValues(properties.path("resultType")))
                .containsExactlyInAnyOrder("PLACE", "ADDRESS");
        assertThat(properties.path("providerPlaceId").path("description").asText())
                .contains("ADDRESS이면 null");
        assertThat(properties.path("category").path("description").asText())
                .contains("ADDRESS이면 null");

        JsonNode responseSchema = findSelectionResponseSchema(openApi);
        assertThat(enumValues(responseSchema.path("properties").path("source")))
                .containsExactlyInAnyOrder(
                        "PLACE_SEARCH",
                        "ADDRESS_SEARCH",
                        "MAP_SELECTION"
                );
    }

    private JsonNode findSearchItemSchema(JsonNode openApi) {
        for (Map.Entry<String, JsonNode> entry :
                openApi.path("components").path("schemas").properties()) {
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

    private JsonNode findParameter(JsonNode operation, String name) {
        for (JsonNode parameter : operation.path("parameters")) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }
        throw new AssertionError("OpenAPI parameter not found: " + name);
    }

    private JsonNode findSelectionRequestSchema(JsonNode openApi) {
        for (Map.Entry<String, JsonNode> entry :
                openApi.path("components").path("schemas").properties()) {
            JsonNode properties = entry.getValue().path("properties");
            if (properties.has("resultType")
                    && properties.has("providerPlaceId")
                    && properties.has("userLatitude")
                    && properties.has("userLongitude")) {
                return entry.getValue();
            }
        }
        throw new AssertionError("Place selection request schema not found");
    }

    private JsonNode findSelectionResponseSchema(JsonNode openApi) {
        for (Map.Entry<String, JsonNode> entry :
                openApi.path("components").path("schemas").properties()) {
            JsonNode properties = entry.getValue().path("properties");
            if (properties.has("source")
                    && properties.has("withinAccessRange")
                    && properties.has("bookmarkedByMe")) {
                return entry.getValue();
            }
        }
        throw new AssertionError("Place selection response schema not found");
    }

    private List<String> enumValues(JsonNode schema) {
        List<String> values = new ArrayList<>();
        schema.path("enum").forEach(value -> values.add(value.asText()));
        return values;
    }
}
