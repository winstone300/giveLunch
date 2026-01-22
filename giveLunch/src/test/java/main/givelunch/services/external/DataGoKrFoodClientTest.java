package main.givelunch.services.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.properties.DataGoKrProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class DataGoKrFoodClientTest {

    @Mock
    private RestTemplate restTemplate;

    // final 제거 (setUp에서 초기화해야 하므로)
    private DataGoKrFoodClient dataGoKrFoodClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DataGoKrProperties properties = new DataGoKrProperties(
            "https://api.example.com",
            "service-key",
            "/foods",
            "json",
            1,
            10,
            1
    );

    @BeforeEach
    void setUp() {
        this.dataGoKrFoodClient = new DataGoKrFoodClient(properties, objectMapper, restTemplate);
    }

    @Test
    @DisplayName("fetchFoodByName: 응답이 정상일 때 항목을 DTO로 변환한다")
    void fetchFoodByNameReturnsDto() {
        // given
        String body = """
                {
                  "body": {
                    "items": [
                      {
                        "FOOD_NM_KR": "비빔밥",
                        "FOOD_OR_NM": "한식",
                        "SERVING_SIZE": "200g",
                        "AMT_NUM1": "550",
                        "AMT_NUM3": "22",
                        "AMT_NUM4": "16",
                        "AMT_NUM6": "77"
                      }
                    ]
                  }
                }
                """;

        when(restTemplate.getForEntity(any(URI.class), any()))
                .thenReturn(ResponseEntity.ok(body));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName("비빔밥");

        // then
        assertThat(result).hasSize(1);
        FoodAndNutritionDto dto = result.get(0);
        NutritionDto nutrition = dto.getNutrition();
        assertThat(dto.getName()).isEqualTo("비빔밥");
        assertThat(dto.getCategory()).isEqualTo("한식");
        assertThat(dto.getServingSizeG()).isEqualTo(200);
        assertThat(nutrition.getCalories()).isEqualByComparingTo(BigDecimal.valueOf(550));
        assertThat(nutrition.getProtein()).isEqualByComparingTo(BigDecimal.valueOf(22));
        assertThat(nutrition.getFat()).isEqualByComparingTo(BigDecimal.valueOf(16));
        assertThat(nutrition.getCarbohydrate()).isEqualByComparingTo(BigDecimal.valueOf(77));

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), any());
        URI expected = UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(properties.getAPI())
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("type", properties.type())
                .queryParam("FOOD_NM_KR", "비빔밥")
                .queryParam("pageNo", properties.pageSize())
                .queryParam("numOfRows", properties.numOfRowsAdmin())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
        assertThat(uriCaptor.getValue()).isEqualTo(expected);
    }

    @Test
    @DisplayName("fetchFoodsByName: items가 비어 있으면 빈 리스트 반환")
    void fetchFoodsByNameReturnsEmptyWhenItemsEmpty() {
        // given
        String body = """
                {
                  "body": {
                    "items": []
                  }
                }
                """;

        when(restTemplate.getForEntity(any(URI.class), any()))
                .thenReturn(ResponseEntity.ok(body));

        //when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName("없는메뉴");

        assertThat(result).isEmpty();
    }
}