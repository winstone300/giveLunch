package main.givelunch.services.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Optional;
import main.givelunch.properties.NaverImageProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class NaverImageClientTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("fetchFirstImageUrl: 자격 증명이 없으면 요청 없이 빈 Optional 반환")
    void fetchFirstImageUrlReturnsEmptyWithoutCredentials() {
        NaverImageProperties properties = new NaverImageProperties(
                "https://openapi.naver.com",
                "/v1/search/image",
                "",
                "",
                1,
                1,
                "sim"
        );
        NaverImageClient client = new NaverImageClient(properties, objectMapper, restTemplate);

        Optional<String> result = client.fetchFirstImageUrl("김치찌개");

        assertThat(result).isEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    @DisplayName("fetchFirstImageUrl: 응답에서 첫 번째 이미지 링크를 반환")
    void fetchFirstImageUrlReturnsFirstLink() {
        NaverImageProperties properties = new NaverImageProperties(
                "https://openapi.naver.com",
                "/v1/search/image",
                "client-id",
                "client-secret",
                1,
                1,
                "sim"
        );
        NaverImageClient client = new NaverImageClient(properties, objectMapper, restTemplate);
        String body = """
                {
                  "items": [
                    { "link": "https://img.example.com/kimchi.jpg" }
                  ]
                }
                """;

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(body));

        Optional<String> result = client.fetchFirstImageUrl("김치찌개");

        assertThat(result).contains("https://img.example.com/kimchi.jpg");
    }
}