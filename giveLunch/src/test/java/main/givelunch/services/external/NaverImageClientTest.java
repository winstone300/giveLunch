package main.givelunch.services.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class NaverImageClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

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
        NaverImageClient client = new NaverImageClient(properties, objectMapper, restClient);

        Optional<String> result = client.fetchFirstImageUrl("김치찌개");

        assertThat(result).isEmpty();
        verifyNoInteractions(restClient);
    }

    @Test
    @DisplayName("fetchFirstImageUrl: 응답에서 첫 번째 이미지 링크를 반환")
    void fetchFirstImageUrlReturnsFirstLink() {
        // given
        NaverImageProperties properties = new NaverImageProperties(
                "https://openapi.naver.com",
                "/v1/search/image",
                "client-id",
                "client-secret",
                1,
                1,
                "sim"
        );
        NaverImageClient client = new NaverImageClient(properties, objectMapper, restClient);

        String body = """
                {
                  "items": [
                    { "link": "https://img.example.com/kimchi.jpg" }
                  ]
                }
                """;

        // 체이닝 시작 (GET)
        when(restClient.get()).thenReturn(requestHeadersUriSpec);

        // URI 설정
        when(requestHeadersUriSpec.uri(any(URI.class))).thenReturn(requestHeadersSpec);

        // 헤더 설정
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);

        // 요청 발사
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // 결과 변환
        when(responseSpec.body(String.class)).thenReturn(body);

        // when
        Optional<String> result = client.fetchFirstImageUrl("김치찌개");

        // then
        assertThat(result).contains("https://img.example.com/kimchi.jpg");
    }
}