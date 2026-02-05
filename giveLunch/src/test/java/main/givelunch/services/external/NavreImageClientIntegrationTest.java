package main.givelunch.services.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import main.givelunch.properties.NaverImageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class NaverImageClientIntegrationTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("fetchFirstImageUrl: 실제 HTTP 요청으로 헤더/쿼리를 포함해 첫 이미지 링크를 반환")
    void fetchFirstImageUrlReturnsFirstLinkViaHttp() throws Exception {
        AtomicReference<String> requestQuery = new AtomicReference<>();
        AtomicReference<String> clientIdHeader = new AtomicReference<>();
        AtomicReference<String> clientSecretHeader = new AtomicReference<>();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/search/image", exchange -> {
            captureRequest(exchange, requestQuery, clientIdHeader, clientSecretHeader);
            String body = """
                    {
                      "items": [
                        {"link": "https://img.example.com/kimchi.jpg"}
                      ]
                    }
                    """;
            respond(exchange, 200, body);
        });
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        NaverImageProperties properties = new NaverImageProperties(
                baseUrl,
                "/v1/search/image",
                "cid",
                "secret",
                1,
                1,
                "sim"
        );

        NaverImageClient client = new NaverImageClient(properties, new ObjectMapper(), RestClient.builder().build());

        Optional<String> result = client.fetchFirstImageUrl("김치찌개");

        assertThat(result).contains("https://img.example.com/kimchi.jpg");
        assertThat(requestQuery.get()).contains("query=");
        assertThat(requestQuery.get()).contains("display=1");
        assertThat(requestQuery.get()).contains("start=1");
        assertThat(requestQuery.get()).contains("sort=sim");
        assertThat(clientIdHeader.get()).isEqualTo("cid");
        assertThat(clientSecretHeader.get()).isEqualTo("secret");
    }

    @Test
    @DisplayName("fetchFirstImageUrl: 외부 API가 500을 반환하면 Optional.empty 반환")
    void fetchFirstImageUrlReturnsEmptyOnServerError() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/search/image", exchange -> respond(exchange, 500, ""));
        server.start();

        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        NaverImageProperties properties = new NaverImageProperties(
                baseUrl,
                "/v1/search/image",
                "cid",
                "secret",
                1,
                1,
                "sim"
        );

        NaverImageClient client = new NaverImageClient(properties, new ObjectMapper(), RestClient.builder().build());

        Optional<String> result = client.fetchFirstImageUrl("김치찌개");

        assertThat(result).isEmpty();
    }

    private void captureRequest(
            HttpExchange exchange,
            AtomicReference<String> query,
            AtomicReference<String> clientId,
            AtomicReference<String> clientSecret
    ) {
        query.set(exchange.getRequestURI().getRawQuery());
        clientId.set(exchange.getRequestHeaders().getFirst("X-Naver-Client-Id"));
        clientSecret.set(exchange.getRequestHeaders().getFirst("X-Naver-Client-Secret"));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}