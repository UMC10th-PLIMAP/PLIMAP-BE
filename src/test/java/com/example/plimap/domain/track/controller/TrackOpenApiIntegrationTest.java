package com.example.plimap.domain.track.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.plimap.domain.track.exception.TrackErrorCode;
import com.example.plimap.global.apiPayload.code.GeneralErrorCode;
import com.example.plimap.support.PostgisContainerConfiguration;
import com.example.plimap.support.RedisContainerConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
class TrackOpenApiIntegrationTest {

    private static final String PLACE_TRACK_PATH =
            "/api/v1/places/{placeId}/tracks";
    private static final String PLACE_TRACK_DETAIL_PATH =
            "/api/v1/place-tracks/{placeTrackId}";
    private static final String PLACE_TRACK_LIKE_PATH =
            "/api/v1/place-tracks/{placeTrackId}/likes";
    private static final String LIKED_PLACE_TRACK_LIST_PATH =
            "/api/v1/place-tracks/likes";
    private static final String TRACK_SEARCH_PATH = "/api/v1/tracks/search";
    private static final String PLAYBACK_PREPARATION_PATH =
            "/api/v1/tracks/playback-preparations";
    private static final String PLAYBACK_FAILURE_PATH =
            "/api/v1/tracks/playback-failures";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void Track_API는_성공과_실패에_맞는_응답_스키마를_참조한다() throws Exception {
        String responseBody = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode openApi = objectMapper.readTree(responseBody);

        assertThat(referenceName(responseSchemaReference(
                openApi,
                PLACE_TRACK_PATH,
                "get",
                "200"
        ))).isEqualTo("ApiResponsePlaceTrackListResult");
        assertThat(referenceName(responseSchemaReference(
                openApi,
                PLACE_TRACK_DETAIL_PATH,
                "get",
                "200"
        ))).isEqualTo("ApiResponsePlaceTrackDetail");
        assertThat(referenceName(responseSchemaReference(
                openApi,
                PLACE_TRACK_LIKE_PATH,
                "put",
                "200"
        ))).isEqualTo("ApiResponsePlaceTrackLikeResult");
        assertThat(referenceName(responseSchemaReference(
                openApi,
                LIKED_PLACE_TRACK_LIST_PATH,
                "get",
                "200"
        ))).isEqualTo("ApiResponseLikedPlaceTrackListResult");
        assertThat(referenceName(responseSchemaReference(
                openApi,
                PLACE_TRACK_LIKE_PATH,
                "delete",
                "200"
        ))).isEqualTo("ApiResponsePlaceTrackLikeResult");
        assertThat(referenceName(responseSchemaReference(
                openApi,
                TRACK_SEARCH_PATH,
                "get",
                "200"
        ))).isEqualTo("ApiResponseTrackSearchResult");
        assertThat(referenceName(responseSchemaReference(
                openApi,
                PLAYBACK_PREPARATION_PATH,
                "post",
                "200"
        ))).isEqualTo("ApiResponsePlaybackPreparationResult");
        assertThat(referenceName(responseSchemaReference(
                openApi,
                PLAYBACK_FAILURE_PATH,
                "post",
                "200"
        ))).isEqualTo("ApiResponseVoid");

        JsonNode playbackFailureSuccessContent = responseContent(
                openApi,
                PLAYBACK_FAILURE_PATH,
                "post",
                "200"
        ).path("application/json");
        JsonNode playbackFailureSuccessExample =
                exampleValues(playbackFailureSuccessContent).getFirst();
        assertThat(playbackFailureSuccessExample.path("isSuccess").asBoolean()).isTrue();
        assertThat(playbackFailureSuccessExample.path("code").asText())
                .isEqualTo("TRACK_PLAYBACK_FAILURE_REPORTED");
        assertThat(playbackFailureSuccessExample.path("message").asText())
                .isEqualTo("YouTube 재생 실패가 보고되었습니다.");
        assertThat(playbackFailureSuccessExample.path("result").isNull()).isTrue();

        JsonNode placeTrackResult =
                responseResultSchema(openApi, PLACE_TRACK_PATH, "get");
        JsonNode placeTrackItems = placeTrackResult
                .path("properties")
                .path("tracks")
                .path("items");
        assertThat(placeTrackResult.path("properties").has("placeName")).isFalse();
        assertThat(placeTrackResult.path("properties").has("createdBy")).isFalse();
        assertThat(placeTrackResult.path("properties").has("isBookmarked")).isFalse();
        assertThat(placeTrackResult.path("properties").has("placeId")).isTrue();
        assertThat(placeTrackResult.path("properties").has("distance")).isTrue();
        assertThat(placeTrackResult.path("properties").has("isWithinRadius")).isTrue();
        assertThat(placeTrackResult.path("properties")
                .has("isTrackDetailAccessible")).isTrue();
        assertThat(placeTrackResult.path("properties").has("page")).isTrue();
        assertThat(placeTrackResult.path("properties").has("size")).isTrue();
        assertThat(placeTrackResult.path("properties").has("hasNext")).isTrue();
        assertThat(referenceName(placeTrackItems)).isEqualTo("PlaceTrackItem");
        JsonNode placeTrackItemSchema = resolveSchema(openApi, placeTrackItems);
        assertThat(placeTrackItemSchema.path("properties").has("pinCount")).isTrue();

        JsonNode placeTrackDetail =
                responseResultSchema(openApi, PLACE_TRACK_DETAIL_PATH, "get");
        List<String> placeTrackDetailFields = new ArrayList<>();
        placeTrackDetail.path("properties").fieldNames()
                .forEachRemaining(placeTrackDetailFields::add);
        assertThat(placeTrackDetailFields).containsExactlyInAnyOrder(
                        "placeTrackId",
                        "trackId",
                        "youtubeVideoId",
                        "title",
                        "artist",
                        "albumImageUrl",
                        "likeCount",
                        "userLike"
                );

        JsonNode placeTrackLikeResult =
                responseResultSchema(openApi, PLACE_TRACK_LIKE_PATH, "put");
        List<String> placeTrackLikeFields = new ArrayList<>();
        placeTrackLikeResult.path("properties").fieldNames()
                .forEachRemaining(placeTrackLikeFields::add);
        assertThat(placeTrackLikeFields).containsExactlyInAnyOrder(
                "placeTrackId",
                "isLiked",
                "likeCount"
        );

        JsonNode likedPlaceTrackList =
                responseResultSchema(openApi, LIKED_PLACE_TRACK_LIST_PATH, "get");
        JsonNode likedPlaceTrackItems = likedPlaceTrackList
                .path("properties")
                .path("tracks")
                .path("items");
        assertThat(referenceName(likedPlaceTrackItems))
                .isEqualTo("LikedPlaceTrackItem");
        JsonNode likedPlaceTrackItemSchema =
                resolveSchema(openApi, likedPlaceTrackItems);
        List<String> likedPlaceTrackFields = new ArrayList<>();
        likedPlaceTrackItemSchema.path("properties").fieldNames()
                .forEachRemaining(likedPlaceTrackFields::add);
        assertThat(likedPlaceTrackFields).containsExactlyInAnyOrder(
                "placeTrackId",
                "trackName",
                "artistName",
                "artworkUrl",
                "likeCount"
        );

        JsonNode trackSearchResult =
                responseResultSchema(openApi, TRACK_SEARCH_PATH, "get");
        JsonNode trackSearchItems = trackSearchResult
                .path("properties")
                .path("tracks")
                .path("items");
        assertThat(referenceName(trackSearchItems)).isEqualTo("TrackSearchItem");

        JsonNode playbackResult =
                responseResultReference(openApi, PLAYBACK_PREPARATION_PATH, "post");
        assertThat(referenceName(playbackResult))
                .isEqualTo("PlaybackPreparationResult");

        assertFailureResponse(
                openApi,
                TRACK_SEARCH_PATH,
                "get",
                "400",
                "ApiResponseTrackSearchResult",
                Set.of("COMMON_400_VALIDATION_FAILED")
        );
        assertFailureResponse(
                openApi,
                TRACK_SEARCH_PATH,
                "get",
                "401",
                "ApiResponseTrackSearchResult",
                Set.of("COMMON_401_UNAUTHORIZED")
        );
        assertFailureResponse(
                openApi,
                TRACK_SEARCH_PATH,
                "get",
                "500",
                "ApiResponseTrackSearchResult",
                Set.of("TRACK_EXTERNAL_API_ERROR")
        );

        assertFailureResponse(
                openApi,
                PLAYBACK_PREPARATION_PATH,
                "post",
                "400",
                "ApiResponsePlaybackPreparationResult",
                Set.of("COMMON_400_VALIDATION_FAILED")
        );
        assertFailureResponse(
                openApi,
                PLAYBACK_PREPARATION_PATH,
                "post",
                "401",
                "ApiResponsePlaybackPreparationResult",
                Set.of("COMMON_401_UNAUTHORIZED")
        );
        assertFailureResponse(
                openApi,
                PLAYBACK_PREPARATION_PATH,
                "post",
                "404",
                "ApiResponsePlaybackPreparationResult",
                Set.of(
                        "TRACK_404_METADATA_CACHE_NOT_FOUND",
                        "TRACK_404_YOUTUBE_MATCH_NOT_FOUND",
                        "TRACK_404_PLAYBACK_UNAVAILABLE"
                )
        );

        assertFailureResponse(
                openApi,
                PLAYBACK_FAILURE_PATH,
                "post",
                "400",
                "ApiResponseVoid",
                Set.of("COMMON_400_VALIDATION_FAILED")
        );
        assertFailureResponse(
                openApi,
                PLAYBACK_FAILURE_PATH,
                "post",
                "401",
                "ApiResponseVoid",
                Set.of("COMMON_401_UNAUTHORIZED")
        );
        assertFailureResponse(
                openApi,
                PLAYBACK_FAILURE_PATH,
                "post",
                "500",
                "ApiResponseVoid",
                Set.of("TRACK_500_CACHE_ERROR")
        );
        assertFailureResponse(
                openApi,
                PLAYBACK_PREPARATION_PATH,
                "post",
                "500",
                "ApiResponsePlaybackPreparationResult",
                Set.of(
                        "TRACK_500_YOUTUBE_EXTERNAL_API_ERROR",
                        "TRACK_500_CACHE_ERROR"
                )
        );

        assertFailureResponse(
                openApi,
                PLACE_TRACK_PATH,
                "get",
                "400",
                "ApiResponsePlaceTrackListResult",
                Set.of("COMMON_400_VALIDATION_FAILED")
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_PATH,
                "get",
                "401",
                "ApiResponsePlaceTrackListResult",
                Set.of("COMMON_401_UNAUTHORIZED")
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_PATH,
                "get",
                "404",
                "ApiResponsePlaceTrackListResult",
                Set.of("PLACE_NOT_FOUND")
        );

        assertFailureResponse(
                openApi,
                PLACE_TRACK_DETAIL_PATH,
                "get",
                "400",
                "ApiResponsePlaceTrackDetail",
                Set.of(
                        GeneralErrorCode.TYPE_MISMATCH.getCode(),
                        GeneralErrorCode.VALIDATION_FAILED.getCode()
                )
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_DETAIL_PATH,
                "get",
                "401",
                "ApiResponsePlaceTrackDetail",
                Set.of(GeneralErrorCode.UNAUTHORIZED.getCode())
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_DETAIL_PATH,
                "get",
                "403",
                "ApiResponseVoid",
                Set.of(TrackErrorCode.PLACE_TRACK_ACCESS_DENIED.getCode())
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_DETAIL_PATH,
                "get",
                "404",
                "ApiResponsePlaceTrackDetail",
                Set.of(TrackErrorCode.PLACE_TRACK_NOT_FOUND.getCode())
        );
        JsonNode placeTrackNotFoundExample = exampleValues(responseContent(
                openApi,
                PLACE_TRACK_DETAIL_PATH,
                "get",
                "404"
        ).path("application/json")).getFirst();
        assertThat(placeTrackNotFoundExample.path("message").asText())
                .isEqualTo(TrackErrorCode.PLACE_TRACK_NOT_FOUND.getMessage());

        assertFailureResponse(
                openApi,
                PLACE_TRACK_LIKE_PATH,
                "put",
                "400",
                "ApiResponsePlaceTrackLikeResult",
                Set.of(
                        GeneralErrorCode.TYPE_MISMATCH.getCode(),
                        GeneralErrorCode.VALIDATION_FAILED.getCode(),
                        TrackErrorCode.PLACE_TRACK_ALREADY_LIKED.getCode()
                )
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_LIKE_PATH,
                "put",
                "401",
                "ApiResponsePlaceTrackLikeResult",
                Set.of(GeneralErrorCode.UNAUTHORIZED.getCode())
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_LIKE_PATH,
                "put",
                "404",
                "ApiResponsePlaceTrackLikeResult",
                Set.of(TrackErrorCode.PLACE_TRACK_NOT_FOUND.getCode())
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_LIKE_PATH,
                "delete",
                "400",
                "ApiResponsePlaceTrackLikeResult",
                Set.of(
                        GeneralErrorCode.TYPE_MISMATCH.getCode(),
                        GeneralErrorCode.VALIDATION_FAILED.getCode()
                )
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_LIKE_PATH,
                "delete",
                "401",
                "ApiResponsePlaceTrackLikeResult",
                Set.of(GeneralErrorCode.UNAUTHORIZED.getCode())
        );
        assertFailureResponse(
                openApi,
                PLACE_TRACK_LIKE_PATH,
                "delete",
                "404",
                "ApiResponsePlaceTrackLikeResult",
                Set.of(
                        TrackErrorCode.PLACE_TRACK_NOT_FOUND.getCode(),
                        TrackErrorCode.PLACE_TRACK_LIKE_NOT_FOUND.getCode()
                )
        );
        assertFailureResponse(
                openApi,
                LIKED_PLACE_TRACK_LIST_PATH,
                "get",
                "400",
                "ApiResponseLikedPlaceTrackListResult",
                Set.of(GeneralErrorCode.VALIDATION_FAILED.getCode())
        );
        assertFailureResponse(
                openApi,
                LIKED_PLACE_TRACK_LIST_PATH,
                "get",
                "401",
                "ApiResponseLikedPlaceTrackListResult",
                Set.of(GeneralErrorCode.UNAUTHORIZED.getCode())
        );
    }

    private void assertFailureResponse(
            JsonNode openApi,
            String path,
            String operation,
            String status,
            String expectedSchemaName,
            Set<String> expectedCodes
    ) {
        JsonNode content = responseContent(openApi, path, operation, status)
                .path("application/json");
        JsonNode schemaReference = content.path("schema");

        assertThat(referenceName(schemaReference)).isEqualTo(expectedSchemaName);
        JsonNode failureSchema = resolveSchema(openApi, schemaReference);
        assertThat(failureSchema.path("properties").has("isSuccess")).isTrue();
        assertThat(failureSchema.path("properties").has("code")).isTrue();
        assertThat(failureSchema.path("properties").has("message")).isTrue();
        assertThat(failureSchema.path("properties").has("result")).isTrue();

        List<JsonNode> examples = exampleValues(content);
        assertThat(examples).isNotEmpty();
        assertThat(examples)
                .allSatisfy(example -> {
                    assertThat(example.path("isSuccess").asBoolean()).isFalse();
                    assertThat(example.path("result").isNull()).isTrue();
                });
        assertThat(examples)
                .extracting(example -> example.path("code").asText())
                .containsExactlyInAnyOrderElementsOf(expectedCodes);
    }

    private List<JsonNode> exampleValues(JsonNode content) {
        List<JsonNode> values = new ArrayList<>();
        content.path("examples").elements()
                .forEachRemaining(example -> values.add(example.path("value")));
        if (values.isEmpty() && content.has("example")) {
            values.add(content.path("example"));
        }
        return values;
    }

    private JsonNode responseResultSchema(
            JsonNode openApi,
            String path,
            String operation
    ) {
        return resolveSchema(
                openApi,
                responseResultReference(openApi, path, operation)
        );
    }

    private JsonNode responseResultReference(
            JsonNode openApi,
            String path,
            String operation
    ) {
        JsonNode responseSchema = responseSchemaReference(
                openApi,
                path,
                operation,
                "200"
        );
        JsonNode apiResponseSchema = resolveSchema(openApi, responseSchema);
        return apiResponseSchema.path("properties").path("result");
    }

    private JsonNode responseSchemaReference(
            JsonNode openApi,
            String path,
            String operation,
            String status
    ) {
        JsonNode content = responseContent(openApi, path, operation, status);
        return content.elements().next().path("schema");
    }

    private JsonNode responseContent(
            JsonNode openApi,
            String path,
            String operation,
            String status
    ) {
        return openApi
                .path("paths")
                .path(path)
                .path(operation)
                .path("responses")
                .path(status)
                .path("content");
    }

    private JsonNode resolveSchema(JsonNode openApi, JsonNode schema) {
        String referenceName = referenceName(schema);
        if (referenceName == null) {
            return schema;
        }
        return openApi.path("components").path("schemas").path(referenceName);
    }

    private String referenceName(JsonNode schema) {
        String reference = schema.path("$ref").asText(null);
        if (reference == null) {
            return null;
        }
        return reference.substring(reference.lastIndexOf('/') + 1);
    }
}
