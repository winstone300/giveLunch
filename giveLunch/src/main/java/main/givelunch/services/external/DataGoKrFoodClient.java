package main.givelunch.services.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.givelunch.dto.FoodAndNutritionDto;
import main.givelunch.dto.NutritionDto;
import main.givelunch.properties.DataGoKrProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataGoKrFoodClient {
    private final DataGoKrProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final NaverImageClient naverImageClient;

    // API요청 관련 문자
    private static final String PARAM_SERVICE_KEY = "serviceKey";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_FOOD_NAME = "FOOD_NM_KR";
    private static final String PARAM_PAGE_NO = "pageNo";
    private static final String PARAM_NUM_OF_ROWS = "numOfRows";
    private static final String DEFAULT_TYPE = "json";  //자료 요청 기본값

    // JSON parsing 경로 키
    private static final String JSON_BODY = "body";
    private static final String JSON_ITEMS = "items";

    // 경고 log
    private static final String FAIL_REQUEST_LOG =  "Data.go.kr API request failed with status={} for name={}";
    private static final String FAIL_PARSING_LOG = "Data.go.kr API parsing failed for name={}";

    // 응답 response source
    private static final String SOURCE_DATA_GO_KR = "outer_db";

    // 기본값으로 넘김
    public List<FoodAndNutritionDto> fetchFoodsByName(String name) {
        return fetchFoodsByName(name, properties.numOfRowsAdmin());
    }

    public List<FoodAndNutritionDto> fetchFoodsByName(String name,int numOfRows) {
        URI uri = buildUri(name,numOfRows);
        return fetchBody(uri, name)
                .map(body -> extractItems(body, name))
                .map(this::mapToDtos)
                .orElseGet(List::of);
    }

    // 요청 uri 빌드
    private URI buildUri(String name, int numOfRows) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(properties.getAPI())
                .queryParam(PARAM_SERVICE_KEY, properties.serviceKey())
                .queryParam(PARAM_TYPE, properties.type() == null ? DEFAULT_TYPE : properties.type())
                .queryParam(PARAM_FOOD_NAME, name)
                .queryParam(PARAM_PAGE_NO, properties.pageSize())
                .queryParam(PARAM_NUM_OF_ROWS, numOfRows)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
    }

    // HTTP 호출 -> 응답 body 반환
    private Optional<String> fetchBody(URI uri, String name) {
        try {
            String body = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(body);
        } catch (RestClientResponseException e) {      // 에로 코드 응답(4xx, 5xx)
            log.warn(FAIL_REQUEST_LOG, e.getStatusCode(), name, e);
            return Optional.empty();
        } catch (RestClientException e) {       // 서버 통신 실패
            log.warn(FAIL_REQUEST_LOG, "UNKNOWN", name, e);
            return Optional.empty();
        }
    }

    // 응답 body에서 첫번째 item 반환
    private List<JsonNode> extractItems(String body, String name) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode itemsNode = root.path(JSON_BODY).path(JSON_ITEMS);

            // 데이터가 없거나 누락된 노드인 경우
            if (itemsNode.isMissingNode() || itemsNode.isNull()) {
                return List.of();
            }

            // 배열인 경우 (다건)
            if (itemsNode.isArray()) {
                List<JsonNode> items = new ArrayList<>();
                itemsNode.forEach(items::add);
                return items;
            }

            // 객체인 경우 (단건)
            if (itemsNode.isObject()) {
                return List.of(itemsNode);
            }

            // 그 외
            return List.of();

        } catch (Exception e) {
            log.warn(FAIL_PARSING_LOG, name, e);
            return List.of();
        }
    }

    private List<FoodAndNutritionDto> mapToDtos(List<JsonNode> items) {
        List<FoodAndNutritionDto> results = new ArrayList<>();
        java.util.Map<String, Optional<String>> imageCache = new java.util.HashMap<>();
        for (JsonNode item : items) {
            String foodName = normalizeFoodName(readFirstText(item, DataGoKrAPIField.FOODNAME.key()));
            if (foodName == null) {
                continue;
            }
            Optional<String> cachedImage = imageCache.computeIfAbsent(
                    foodName,
                    naverImageClient::fetchFirstImageUrl
            );
            mapToDto(item, foodName, cachedImage.orElse(null)).ifPresent(results::add);
        }
        return results;
    }


    // 응답 값 dto에 mapping
    private Optional<FoodAndNutritionDto> mapToDto(JsonNode itemNode, String foodName, String imgUrl) {
        if (foodName == null || foodName.isBlank()) {
            return Optional.empty();
        }

        String category = readFirstText(itemNode, DataGoKrAPIField.CATEGORY.key());
        Integer servingSizeG = parseInteger(readFirstText(itemNode, DataGoKrAPIField.SERVINGSIZE.key()));

        BigDecimal calories = parseBigDecimal(readFirstText(itemNode, DataGoKrAPIField.CALORIES.key()));
        BigDecimal carbohydrate = parseBigDecimal(readFirstText(itemNode, DataGoKrAPIField.CARBOHYDRATE.key()));
        BigDecimal protein = parseBigDecimal(readFirstText(itemNode, DataGoKrAPIField.PROTEIN.key()));
        BigDecimal fat = parseBigDecimal(readFirstText(itemNode, DataGoKrAPIField.FAT.key()));

        NutritionDto nutritionDto =
                (calories != null || carbohydrate != null || protein != null || fat != null)
                        ? NutritionDto.of(calories, protein, fat, carbohydrate)
                        : null;

        return Optional.of(FoodAndNutritionDto.of(
                null,
                foodName,
                category,
                imgUrl,
                servingSizeG,
                nutritionDto,
                SOURCE_DATA_GO_KR
        ));
    }

    private String normalizeFoodName(String foodName) {
        if (foodName == null) {
            return null;
        }
        String trimmed = foodName.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    // 키 값중 첫번째 값 반환
    private String readFirstText(JsonNode node, String key) {
        JsonNode valueNode = node.path(key);
        if (!valueNode.isMissingNode() && !valueNode.isNull()) {
            String value = valueNode.asText();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    // 0-9와 .(소수점)만 남김
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    //0-9만 남김
    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        return Integer.valueOf(normalized);
    }
}