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

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    private List<String> enumValues(JsonNode schema) {
        List<String> values = new ArrayList<>();
        schema.path("enum").forEach(value -> values.add(value.asText()));
        return values;
    }
}
