package com.example.plimap.domain.track.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AlbumImageUrlResolverTest {

    private static final String ORIGINAL_URL =
            "https://is1-ssl.mzstatic.com/image/thumb/album/100x100bb.jpg";
    private static final String HIGH_RESOLUTION_URL =
            "https://is1-ssl.mzstatic.com/image/thumb/album/600x600bb.jpg";

    private MockRestServiceServer server;
    private AlbumImageUrlResolver resolver;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        resolver = new AlbumImageUrlResolver(builder.build());
    }

    @Test
    void 고해상도_URL이_2xx이면_600x600_URL을_반환한다() {
        server.expect(once(), requestTo(HIGH_RESOLUTION_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.RANGE, "bytes=0-0"))
                .andRespond(withSuccess());

        String result = resolver.resolve(ORIGINAL_URL);

        assertThat(result).isEqualTo(HIGH_RESOLUTION_URL);
        server.verify();
    }

    @Test
    void 마지막_path_segment만_변환하고_query_parameter는_유지한다() {
        String originalUrl = ORIGINAL_URL + "?source=100x100";
        String highResolutionUrl = HIGH_RESOLUTION_URL + "?source=100x100";
        server.expect(once(), requestTo(highResolutionUrl))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        String result = resolver.resolve(originalUrl);

        assertThat(result).isEqualTo(highResolutionUrl);
        server.verify();
    }

    @Test
    void bb가_없는_webp_URL도_600x600으로_변환한다() {
        String originalUrl = "https://is2-ssl.mzstatic.com/100x100.webp";
        String highResolutionUrl = "https://is2-ssl.mzstatic.com/600x600.webp";
        server.expect(once(), requestTo(highResolutionUrl))
                .andRespond(withSuccess());

        String result = resolver.resolve(originalUrl);

        assertThat(result).isEqualTo(highResolutionUrl);
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://is1-ssl.mzstatic.com/image/thumb/album/100x100bb.jpg",
            "https://127.0.0.1/image/thumb/album/100x100bb.jpg",
            "https://localhost/image/thumb/album/100x100bb.jpg",
            "https://evil-mzstatic.com/image/thumb/album/100x100bb.jpg",
            "https://mzstatic.com.evil.com/image/thumb/album/100x100bb.jpg"
    })
    void 허용되지_않은_URL이면_HTTP_요청_없이_원본_URL을_반환한다(String originalUrl) {
        server.expect(never(), request -> { });

        String result = resolver.resolve(originalUrl);

        assertThat(result).isEqualTo(originalUrl);
        server.verify();
    }

    @Test
    void 잘못된_URI이면_예외를_전파하지_않고_원본_URL을_반환한다() {
        String originalUrl = "https://[invalid]/image/thumb/album/100x100bb.jpg";
        server.expect(never(), request -> { });

        String result = resolver.resolve(originalUrl);

        assertThat(result).isEqualTo(originalUrl);
        server.verify();
    }

    @Test
    void 고해상도_URL이_404이면_원본_URL을_반환한다() {
        server.expect(once(), requestTo(HIGH_RESOLUTION_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        String result = resolver.resolve(ORIGINAL_URL);

        assertThat(result).isEqualTo(ORIGINAL_URL);
        server.verify();
    }

    @Test
    void 고해상도_URL_요청에서_예외가_발생하면_원본_URL을_반환한다() {
        server.expect(once(), requestTo(HIGH_RESOLUTION_URL))
                .andRespond(withException(new SocketTimeoutException("read timed out")));

        String result = resolver.resolve(ORIGINAL_URL);

        assertThat(result).isEqualTo(ORIGINAL_URL);
        server.verify();
    }

    @Test
    void 원본_URL이_null이면_null을_반환하고_HTTP_요청을_하지_않는다() {
        server.expect(never(), request -> { });

        String result = resolver.resolve(null);

        assertThat(result).isNull();
        server.verify();
    }

    @Test
    void 마지막_path_segment가_변환할_수_없는_형태면_원본_URL을_반환한다() {
        String originalUrl = "https://image.example/200x200bb.jpg?size=100x100";
        server.expect(never(), request -> { });

        String result = resolver.resolve(originalUrl);

        assertThat(result).isEqualTo(originalUrl);
        server.verify();
    }
}
