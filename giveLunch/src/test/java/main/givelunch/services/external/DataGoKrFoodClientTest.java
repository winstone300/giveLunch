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

    @Mock
    private NaverImageClient naverImageClient;

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
        this.dataGoKrFoodClient = new DataGoKrFoodClient(properties, objectMapper, restTemplate,naverImageClient);
    }

    @Test
    @DisplayName("fetchFoodByName(name): name 인자만 제공시 admin용 설정 개수 요청 후 dto 변환")
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
        when(naverImageClient.fetchFirstImageUrl("비빔밥"))
                .thenReturn(Optional.of("https://img.example.com/bibimbap.jpg"));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName("비빔밥");

        // then
        assertThat(result).hasSize(1);
        FoodAndNutritionDto dto = result.get(0);
        NutritionDto nutrition = dto.getNutrition();
        assertThat(dto.getName()).isEqualTo("비빔밥");
        assertThat(dto.getCategory()).isEqualTo("한식");
        assertThat(dto.getImgUrl()).isEqualTo("https://img.example.com/bibimbap.jpg");
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
    @DisplayName("fetchFoodsByName(name, rows): 직접 지정한 행 개수(numOfRows)가 URI 파라미터로 전달된다")
    void fetchFoodsByNameWithCustomRows() {
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

        // when
        // 2개의 인자를 받는 메서드를 직접 호출
        int customRows = 50;
        dataGoKrFoodClient.fetchFoodsByName("라면", customRows);

        // then
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).getForEntity(uriCaptor.capture(), any());

        URI capturedUri = uriCaptor.getValue();
        assertThat(capturedUri.getQuery()).contains("numOfRows=" + customRows);
        assertThat(capturedUri.getQuery()).contains("FOOD_NM_KR=라면");
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

    @Test
    @DisplayName("fetchFoodsByName: 외부 API 에러(4xx, 5xx) 발생 시 빈 리스트 반환")
    void fetchFoodsByNameReturnsEmptyOnHttpError() {
        // given
        when(restTemplate.getForEntity(any(URI.class), any()))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.BAD_REQUEST));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName("오류발생");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchFoodsByName: 통신 오류(RestClientException) 발생 시 빈 리스트 반환")
    void fetchFoodsByNameReturnsEmptyOnConnectionError() {
        // given
        when(restTemplate.getForEntity(any(URI.class), any()))
                .thenThrow(new org.springframework.web.client.RestClientException("Connection refused"));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName("통신실패");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchFoodsByName: JSON 형식이 올바르지 않으면 파싱 실패 로직을 타고 빈 리스트 반환")
    void fetchFoodsByNameReturnsEmptyOnInvalidJson() {
        // given: 닫는 괄호가 없는 잘못된 JSON
        String brokenJson = "{ \"body\": { \"items\": [ ... ";

        when(restTemplate.getForEntity(any(URI.class), any()))
                .thenReturn(ResponseEntity.ok(brokenJson));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName("파싱실패");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("fetchFoodsByName: 필수 값이 없으면 스킵하고, 숫자에 문자가 섞여 있으면 정제하여 매핑한다")
    void fetchFoodsByNameHandlesDirtyDataAndMissingFields() {
        // given
        String body = """
                {
                  "body": {
                    "items": [
                      {
                        "FOOD_OR_NM": "한식",
                        "AMT_NUM1": "500"
                      },
                      {
                        "FOOD_NM_KR": "치킨",
                        "SERVING_SIZE": "200g",
                        "AMT_NUM1": "1,500 kcal", 
                        "AMT_NUM3": "20g",
                        "AMT_NUM4": "15 g",
                        "AMT_NUM6": "70"
                      }
                    ]
                  }
                }
                """;

        when(restTemplate.getForEntity(any(URI.class), any()))
                .thenReturn(ResponseEntity.ok(body));
        when(naverImageClient.fetchFirstImageUrl("치킨"))
                .thenReturn(Optional.of("https://img.example.com/chicken.jpg"));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName("치킨");

        // then
        assertThat(result).hasSize(1);

        FoodAndNutritionDto dto = result.get(0);
        assertThat(dto.getName()).isEqualTo("치킨");
        assertThat(dto.getServingSizeG()).isEqualTo(200); // 200g -> 200
        assertThat(dto.getImgUrl()).isEqualTo("https://img.example.com/chicken.jpg");
        // 1,500 kcal -> 1500
        assertThat(dto.getNutrition().getCalories()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        assertThat(dto.getNutrition().getProtein()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }
}