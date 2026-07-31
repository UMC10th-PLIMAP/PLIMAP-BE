package com.example.plimap.global.external.kakao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

class KakaoCoordinateClientImplTest {

    private MockRestServiceServer server;
    private KakaoCoordinateClientImpl client;

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
        client = new KakaoCoordinateClientImpl(builder.build(), properties);
    }

    @Test
    void 행정구역_조회에_인증과_경위도를_전달한다() {
        server.expect(once(), request -> {
                    var parameters = UriComponentsBuilder.fromUri(request.getURI())
                            .build()
                            .getQueryParams();
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/v2/local/geo/coord2regioncode.json");
                    assertThat(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                            .isEqualTo("KakaoAK test-rest-api-key");
                    assertThat(parameters.getFirst("x")).isEqualTo("126.9326");
                    assertThat(parameters.getFirst("y")).isEqualTo("37.5283");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(regionResponse(), MediaType.APPLICATION_JSON));

        var response = client.getRegionCodes(37.5283, 126.9326);

        assertThat(response.documents()).hasSize(2);
        assertThat(response.documents().get(1).regionType()).isEqualTo("H");
        assertThat(response.documents().get(1).code()).isEqualTo("1156054000");
        server.verify();
    }

    @Test
    void 주소_조회에서_건물명과_지번_도로명_주소를_역직렬화한다() {
        server.expect(once(), request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/v2/local/geo/coord2address.json");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(addressResponse(), MediaType.APPLICATION_JSON));

        var document = client.getAddress(37.5283, 126.9326).documents().getFirst();

        assertThat(document.address().addressName())
                .isEqualTo("서울 영등포구 여의도동 84-1");
        assertThat(document.roadAddress().addressName())
                .isEqualTo("서울 영등포구 여의동로 330");
        assertThat(document.roadAddress().buildingName()).isEqualTo("물빛무대");
        server.verify();
    }

    @Test
    void 카카오_HTTP_오류를_외부_API_예외로_변환한다() {
        server.expect(once(), request -> { })
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.getRegionCodes(37.5283, 126.9326))
                .isInstanceOf(KakaoClientException.class)
                .isNotInstanceOf(KakaoClientTimeoutException.class);
        server.verify();
    }

    @Test
    void 카카오_timeout을_timeout_예외로_변환한다() {
        server.expect(once(), request -> { })
                .andRespond(withException(new SocketTimeoutException("read timed out")));

        assertThatThrownBy(() -> client.getAddress(37.5283, 126.9326))
                .isInstanceOf(KakaoClientTimeoutException.class);
        server.verify();
    }

    @Test
    void 카카오_응답을_역직렬화할_수_없으면_외부_API_예외로_변환한다() {
        server.expect(once(), request -> { })
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getRegionCodes(37.5283, 126.9326))
                .isInstanceOf(KakaoClientException.class)
                .isNotInstanceOf(KakaoClientTimeoutException.class);
        server.verify();
    }

    private String regionResponse() {
        return """
                {
                  "documents": [
                    {
                      "region_type": "B",
                      "code": "1156011000",
                      "region_1depth_name": "서울특별시",
                      "region_2depth_name": "영등포구",
                      "region_3depth_name": "여의도동"
                    },
                    {
                      "region_type": "H",
                      "code": "1156054000",
                      "region_1depth_name": "서울특별시",
                      "region_2depth_name": "영등포구",
                      "region_3depth_name": "여의동"
                    }
                  ]
                }
                """;
    }

    private String addressResponse() {
        return """
                {
                  "documents": [
                    {
                      "address": {
                        "address_name": "서울 영등포구 여의도동 84-1"
                      },
                      "road_address": {
                        "address_name": "서울 영등포구 여의동로 330",
                        "building_name": "물빛무대"
                      }
                    }
                  ]
                }
                """;
    }
}
