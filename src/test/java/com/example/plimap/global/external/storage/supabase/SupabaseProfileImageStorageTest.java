package com.example.plimap.global.external.storage.supabase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.plimap.global.external.storage.ProfileImageStorageException;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseProfileImageStorageTest {

    private static final String SUPABASE_URL = "https://project.supabase.co";
    private static final String BUCKET = "profile-images";
    private static final String SECRET_KEY = "sb_secret_test-value";
    private static final String OBJECT_KEY =
            "members/42/123e4567-e89b-42d3-a456-426614174000.webp";
    private static final String OBJECT_URL =
            SUPABASE_URL + "/storage/v1/object/" + BUCKET + "/" + OBJECT_KEY;

    private MockRestServiceServer server;
    private SupabaseProfileImageStorage storage;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        storage = new SupabaseProfileImageStorage(builder.build(), properties());
    }

    @Test
    void Secret_key를_apikey_헤더로_전달해_이미지를_업로드한다() {
        byte[] image = {1, 2, 3};

        server.expect(once(), requestTo(OBJECT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    HttpHeaders headers = request.getHeaders();
                    assertThat(headers.getFirst("apikey")).isEqualTo(SECRET_KEY);
                    assertThat(headers.containsHeader(HttpHeaders.AUTHORIZATION)).isFalse();
                    assertThat(headers.getFirst("x-upsert")).isEqualTo("false");
                    assertThat(headers.getFirst(HttpHeaders.CACHE_CONTROL))
                            .isEqualTo("31536000");
                    assertThat(headers.getContentType())
                            .isEqualTo(MediaType.parseMediaType("image/webp"));
                })
                .andExpect(content().bytes(image))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        storage.upload(
                OBJECT_KEY,
                image,
                MediaType.parseMediaType("image/webp")
        );

        server.verify();
    }

    @Test
    void 객체_키를_본문에_담아_이미지를_삭제한다() {
        server.expect(once(), requestTo(
                        SUPABASE_URL + "/storage/v1/object/" + BUCKET
                ))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(request -> {
                    assertThat(request.getHeaders().getFirst("apikey"))
                            .isEqualTo(SECRET_KEY);
                    assertThat(request.getHeaders().containsHeader(HttpHeaders.AUTHORIZATION))
                            .isFalse();
                })
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(
                        "{\"prefixes\":[\"" + OBJECT_KEY + "\"]}"
                ))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        storage.delete(OBJECT_KEY);

        server.verify();
    }

    @Test
    void public_버킷의_공개_URL을_인증정보_없이_조합한다() {
        URI publicUrl = storage.getPublicUrl(OBJECT_KEY);

        assertThat(publicUrl).hasToString(
                SUPABASE_URL + "/storage/v1/object/public/" + BUCKET + "/" + OBJECT_KEY
        );
    }

    @Test
    void 업로드_HTTP_오류를_내부_예외로_변환하고_Secret을_노출하지_않는다() {
        server.expect(once(), requestTo(OBJECT_URL))
                .andRespond(withServerError());

        assertThatThrownBy(() -> storage.upload(
                OBJECT_KEY,
                new byte[]{1},
                MediaType.parseMediaType("image/webp")
        )).isInstanceOfSatisfying(ProfileImageStorageException.class, exception ->
                assertThat(exception.getMessage())
                        .doesNotContain(SECRET_KEY)
                        .isEqualTo("프로필 이미지 업로드에 실패했습니다.")
        );
        server.verify();
    }

    @Test
    void 삭제_HTTP_오류를_내부_예외로_변환하고_Secret을_노출하지_않는다() {
        server.expect(once(), requestTo(
                        SUPABASE_URL + "/storage/v1/object/" + BUCKET
                ))
                .andRespond(withServerError());

        assertThatThrownBy(() -> storage.delete(OBJECT_KEY))
                .isInstanceOfSatisfying(ProfileImageStorageException.class, exception ->
                        assertThat(exception.getMessage())
                                .doesNotContain(SECRET_KEY)
                                .isEqualTo("프로필 이미지 삭제에 실패했습니다.")
                );
        server.verify();
    }

    @Test
    void 허용된_형식이_아닌_객체_키는_요청하지_않는다() {
        assertThatThrownBy(() -> storage.getPublicUrl("../secret"))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    private ProfileImageStorageProperties properties() {
        return new ProfileImageStorageProperties(
                ProfileImageStorageProperties.Provider.SUPABASE,
                BUCKET,
                Duration.ofSeconds(2),
                Duration.ofSeconds(5),
                31_536_000,
                new ProfileImageStorageProperties.Supabase(
                        URI.create(SUPABASE_URL),
                        SECRET_KEY
                )
        );
    }
}
