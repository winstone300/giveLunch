package main.givelunch.services.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.properties.DataGoKrProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@ExtendWith(MockitoExtension.class)
class DataGoKrFoodClientTest {

    private DataGoKrFoodClient dataGoKrFoodClient;
    private MockRestServiceServer mockServer;

    @Mock
    private NaverImageClient naverImageClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 테스트용 프로퍼티 (url, key, api, type, pageSize, numOfRowsAdmin, numOfRowsUser)
    private final DataGoKrProperties properties = new DataGoKrProperties(
            "https://api.example.com", "service-key", "/foods", "json", 1, 10, 1
    );

    @BeforeEach
    void setUp() {
        // 1. RestClient 빌더 및 가짜 서버 설정
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        // 2. Client 주입
        this.dataGoKrFoodClient = new DataGoKrFoodClient(properties, objectMapper, restClient, naverImageClient);
    }

    // 검증용 url 생성
    private String createExpectedUrl(String name, int numOfRows) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(properties.getAPI())
                .queryParam("serviceKey", properties.serviceKey())
                .queryParam("type", properties.type())
                .queryParam("FOOD_NM_KR", name)
                .queryParam("pageNo", properties.pageSize())
                .queryParam("numOfRows", numOfRows)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    @Test
    @DisplayName("[다건] JSON items가 배열(Array)로 올 때")
    void fetchFoodByNameReturnsDtoList_WhenArray() {
        // given
        String foodName = "비빔밥";
        String body = """
                { 
                    "body": { 
                        "items": [ 
                            { "FOOD_NM_KR": "전주비빔밥", "SERVING_SIZE": "200g" },
                            { "FOOD_NM_KR": "참치비빔밥", "SERVING_SIZE": "250g" }
                        ] 
                    } 
                }
                """;

        when(naverImageClient.fetchFirstImageUrl(anyString()))
                .thenReturn(Optional.of("https://img.example.com/default.jpg"));

        // Admin용 기본 개수(10)로 URL 매칭
        String expectedUrl = createExpectedUrl(foodName, properties.numOfRowsAdmin());

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName(foodName);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("전주비빔밥");
        assertThat(result.get(1).getName()).isEqualTo("참치비빔밥");
        mockServer.verify();
    }

    @Test
    @DisplayName("[다건] JSON items가 배열(Array)로 올 때 - fetchFoodsByName 두 인자 넣을때")
    void fetchFoodByNameReturnsDtoList_WhenArray_WithNumOfRows() {
        // given
        String foodName = "비빔밥";
        String body = """
                { 
                    "body": { 
                        "items": [ 
                            { "FOOD_NM_KR": "전주비빔밥", "SERVING_SIZE": "200g" },
                            { "FOOD_NM_KR": "참치비빔밥", "SERVING_SIZE": "250g" }
                        ] 
                    } 
                }
                """;

        when(naverImageClient.fetchFirstImageUrl(anyString()))
                .thenReturn(Optional.of("https://img.example.com/default.jpg"));

        // Admin용 기본 개수(10)로 URL 매칭
        String expectedUrl = createExpectedUrl(foodName, properties.numOfRowsUser());

        mockServer.expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName(foodName,properties.numOfRowsUser());

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("전주비빔밥");
        assertThat(result.get(1).getName()).isEqualTo("참치비빔밥");
        mockServer.verify();
    }

    @Test
    @DisplayName("[단건] JSON items가 객체(Object)로 올 때")
    void fetchFoodByNameReturnsDtoList_WhenSingleObject() {
        // given
        String foodName = "라면";
        String body = """
                { 
                    "body": { 
                        "items": { "FOOD_NM_KR": "라면", "SERVING_SIZE": "500g" } 
                    } 
                }
                """;

        when(naverImageClient.fetchFirstImageUrl(anyString())).thenReturn(Optional.empty());

        mockServer.expect(requestTo(createExpectedUrl(foodName, properties.numOfRowsAdmin())))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName(foodName);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("라면");
        mockServer.verify();
    }

    @Test
    @DisplayName("[예외] 검색 결과가 없거나(null/empty) items 구조가 다를 때 빈 리스트 반환")
    void fetchFoodsByNameReturnsEmpty_WhenItemsEmpty() {
        // given
        String foodName = "없는음식";
        // items가 null 이거나 비어있는 경우
        String body = """
                { "body": { "items": null } }
                """;

        // 인코딩된 URL 매칭 ("없는음식" -> "%EC%97%86...")
        mockServer.expect(requestTo(createExpectedUrl(foodName, properties.numOfRowsAdmin())))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName(foodName);

        // then
        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("[에러] 외부 API가 4xx 에러를 반환하면 빈 리스트를 반환하고 로그를 남김")
    void fetchFoodsByNameReturnsEmpty_On4xxError() {
        // given
        String foodName = "에러유발";
        mockServer.expect(requestTo(createExpectedUrl(foodName, properties.numOfRowsAdmin())))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest());

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName(foodName);

        // then
        assertThat(result).isEmpty();
        mockServer.verify();
    }

    @Test
    @DisplayName("[에러] 외부 API가 5xx 에러를 반환하면 빈 리스트를 반환")
    void fetchFoodsByNameReturnsEmpty_On5xxError() {
        // given
        String foodName = "서버에러";
        mockServer.expect(requestTo(createExpectedUrl(foodName, properties.numOfRowsAdmin())))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        // when
        List<FoodAndNutritionDto> result = dataGoKrFoodClient.fetchFoodsByName(foodName);

        // then
        assertThat(result).isEmpty();
        mockServer.verify();
    }
}