package com.example.plimap.global.external.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.plimap.global.external.kakao.dto.KakaoAddressSearchResponse;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

class KakaoAddressSearchClientImplTest {

    private MockRestServiceServer server;
    private KakaoAddressSearchClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://kakao.test");
        server = MockRestServiceServer.bindTo(builder).build();
        KakaoLocalProperties properties = new KakaoLocalProperties(
                URI.create("https://kakao.test"),
                "test-rest-api-key",
                Duration.ofSeconds(2),
                Duration.ofSeconds(5)
        );
        client = new KakaoAddressSearchClientImpl(builder.build(), properties);
    }

    @Test
    void 카카오_주소_검색에_endpoint_인증_query와_size를_전달한다() {
        server.expect(once(), request -> {
                    var parameters = UriComponentsBuilder.fromUri(request.getURI())
                            .build()
                            .getQueryParams();
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/v2/local/search/address.json");
                    assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                            .isEqualTo("KakaoAK test-rest-api-key");
                    assertThat(UriUtils.decode(
                            parameters.getFirst("query"),
                            StandardCharsets.UTF_8
                    )).isEqualTo("서울 영등포구 여의도동 84");
                    assertThat(parameters.getFirst("size")).isEqualTo("15");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        KakaoAddressSearchResponse response =
                client.search("서울 영등포구 여의도동 84");

        assertThat(response.documents()).containsExactly(
                new KakaoAddressSearchResponse.Document(
                        "서울특별시 영등포구 여의도동 84",
                        "126.9326",
                        "37.5283",
                        new KakaoAddressSearchResponse.Address(
                                "서울특별시 영등포구 여의도동 84"
                        ),
                        new KakaoAddressSearchResponse.RoadAddress(
                                "서울특별시 영등포구 여의동로 330"
                        )
                )
        );
        server.verify();
    }

    @Test
    void 카카오_주소_검색_HTTP_오류를_외부_API_예외로_변환한다() {
        server.expect(once(), request -> { })
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.search("여의도동 84"))
                .isInstanceOf(KakaoClientException.class)
                .isNotInstanceOf(KakaoClientTimeoutException.class);
        server.verify();
    }

    @Test
    void 카카오_주소_검색_timeout을_timeout_예외로_변환한다() {
        server.expect(once(), request -> { })
                .andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.search("여의도동 84"))
                .isInstanceOf(KakaoClientTimeoutException.class);
        server.verify();
    }

    @Test
    void 카카오_주소_검색_응답을_역직렬화할_수_없으면_외부_API_예외를_던진다() {
        server.expect(once(), request -> { })
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("여의도동 84"))
                .isInstanceOf(KakaoClientException.class);
        server.verify();
    }

    private String successResponse() {
        return """
                {
                  "documents": [
                    {
                      "address_name": "서울특별시 영등포구 여의도동 84",
                      "x": "126.9326",
                      "y": "37.5283",
                      "address": {
                        "address_name": "서울특별시 영등포구 여의도동 84"
                      },
                      "road_address": {
                        "address_name": "서울특별시 영등포구 여의동로 330"
                      }
                    }
                  ]
                }
                """;
    }
}
