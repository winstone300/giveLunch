package main.givelunch.services.external;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import main.givelunch.dto.FoodAndNutritionDto.FoodAndNutritionDto;
import main.givelunch.properties.DataGoKrProperties;
import main.givelunch.properties.NaverImageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class DataGoKrFoodClientIntegrationTest {

    private HttpServer dataGoKrServer;
    private HttpServer naverServer;

    @AfterEach
    void tearDown() {
        if (dataGoKrServer != null) {
            dataGoKrServer.stop(0);
        }
        if (naverServer != null) {
            naverServer.stop(0);
        }
    }

    @Test
    @DisplayName("fetchFoodsByName: 실제 HTTP 응답(JSON)을 DTO로 변환하고 이미지 API를 연동")
    void fetchFoodsByNameMapsResponseViaHttp() throws Exception {
        AtomicReference<String> dataGoKrQuery = new AtomicReference<>();

        naverServer = HttpServer.create(new InetSocketAddress(0), 0);
        naverServer.createContext("/v1/search/image", exchange -> {
            String body = """
                    {
                      "items": [
                        {"link": "https://img.example.com/bibimbap.jpg"}
                      ]
                    }
                    """;
            respond(exchange, 200, body);
        });
        naverServer.start();

        dataGoKrServer = HttpServer.create(new InetSocketAddress(0), 0);
        dataGoKrServer.createContext("/foods", exchange -> {
            dataGoKrQuery.set(exchange.getRequestURI().getRawQuery());
            String body = """
                    {
                      "body": {
                        "items": [
                          {
                            "FOOD_NM_KR": "비빔밥",
                            "FOOD_OR_NM": "한식",
                            "SERVING_SIZE": "210g",
                            "AMT_NUM1": "450.2kcal",
                            "AMT_NUM3": "12.3g",
                            "AMT_NUM4": "8.1g",
                            "AMT_NUM6": "65.4g"
                          }
                        ]
                      }
                    }
                    """;
            respond(exchange, 200, body);
        });
        dataGoKrServer.start();

        RestClient restClient = RestClient.builder().build();
        NaverImageClient naverImageClient = new NaverImageClient(
                new NaverImageProperties(
                        "http://localhost:" + naverServer.getAddress().getPort(),
                        "/v1/search/image",
                        "cid",
                        "secret",
                        1,
                        1,
                        "sim"
                ),
                new ObjectMapper(),
                restClient
        );
        DataGoKrFoodClient dataGoKrFoodClient = new DataGoKrFoodClient(
                new DataGoKrProperties(
                        "http://localhost:" + dataGoKrServer.getAddress().getPort(),
                        "service-key",
                        "/foods",
                        "json",
                        1,
                        10,
                        1
                ),
                new ObjectMapper(),
                restClient,
                naverImageClient
        );

        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName("비빔밥", 3);

        assertThat(result).hasSize(1);
        FoodAndNutritionDto dto = result.get(0);
        assertThat(dto.name()).isEqualTo("비빔밥");
        assertThat(dto.category()).isEqualTo("한식");
        assertThat(dto.servingSizeG()).isEqualTo(210);
        assertThat(dto.imgUrl()).isEqualTo("https://img.example.com/bibimbap.jpg");
        assertThat(dto.nutrition().calories()).hasToString("450.2");
        assertThat(dto.nutrition().protein()).hasToString("12.3");
        assertThat(dto.nutrition().fat()).hasToString("8.1");
        assertThat(dto.nutrition().carbohydrate()).hasToString("65.4");
        assertThat(dto.source()).isEqualTo("outer_db");

        assertThat(dataGoKrQuery.get()).contains("FOOD_NM_KR=");
        assertThat(dataGoKrQuery.get()).contains("numOfRows=3");
        assertThat(dataGoKrQuery.get()).contains("serviceKey=service-key");
    }

    @Test
    @DisplayName("fetchFoodsByName: 외부 API가 500 응답을 주면 빈 리스트 반환")
    void fetchFoodsByNameReturnsEmptyOnServerError() throws Exception {
        dataGoKrServer = HttpServer.create(new InetSocketAddress(0), 0);
        dataGoKrServer.createContext("/foods", exchange -> respond(exchange, 500, ""));
        dataGoKrServer.start();

        RestClient restClient = RestClient.builder().build();
        NaverImageClient naverImageClient = new NaverImageClient(
                new NaverImageProperties(
                        "http://localhost:9999",
                        "/v1/search/image",
                        "cid",
                        "secret",
                        1,
                        1,
                        "sim"
                ),
                new ObjectMapper(),
                restClient
        );

        DataGoKrFoodClient dataGoKrFoodClient = new DataGoKrFoodClient(
                new DataGoKrProperties(
                        "http://localhost:" + dataGoKrServer.getAddress().getPort(),
                        "service-key",
                        "/foods",
                        "json",
                        1,
                        10,
                        1
                ),
                new ObjectMapper(),
                restClient,
                naverImageClient
        );

        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName("비빔밥", 3);

        assertThat(result).isEmpty();
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}