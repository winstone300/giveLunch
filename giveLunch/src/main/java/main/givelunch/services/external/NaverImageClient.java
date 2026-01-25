package main.givelunch.services.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.givelunch.properties.NaverImageProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
@Slf4j
public class NaverImageClient {
    private static final String HEADER_CLIENT_ID = "X-Naver-Client-Id";
    private static final String HEADER_CLIENT_SECRET = "X-Naver-Client-Secret";
    private static final String PARAM_QUERY = "query";
    private static final String PARAM_DISPLAY = "display";
    private static final String PARAM_START = "start";
    private static final String PARAM_SORT = "sort";
    private static final String JSON_ITEMS = "items";
    private static final String JSON_LINK = "link";
    private static final String FAIL_REQUEST_LOG = "Naver image API request failed with status={} for query={}";
    private static final String FAIL_PARSING_LOG = "Naver image API parsing failed for query={}";

    private final NaverImageProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public Optional<String> fetchFirstImageUrl(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        if (!hasCredentials()) {
            return Optional.empty();
        }
        String trimmed = query.trim();
        URI uri = buildUri(trimmed);
        return fetchBody(uri, trimmed).flatMap(body -> parseFirstLink(body, trimmed));
    }

    private URI buildUri(String query) {
        return UriComponentsBuilder.fromUriString(properties.baseUrl())
                .path(properties.searchPath())
                .queryParam(PARAM_QUERY, query)
                .queryParam(PARAM_DISPLAY, defaultIfNull(properties.display(), 1))
                .queryParam(PARAM_START, defaultIfNull(properties.start(), 1))
                .queryParam(PARAM_SORT, properties.sort())
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUri();
    }

    private Optional<String> fetchBody(URI uri, String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER_CLIENT_ID, properties.clientId());
        headers.set(HEADER_CLIENT_SECRET, properties.clientSecret());
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return Optional.empty();
            }
            return Optional.of(response.getBody());
        } catch (HttpStatusCodeException e) {
            log.warn(FAIL_REQUEST_LOG, e.getStatusCode(), query, e);
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn(FAIL_REQUEST_LOG, "UNKNOWN", query, e);
            return Optional.empty();
        }
    }

    private Optional<String> parseFirstLink(String body, String query) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode itemsNode = root.path(JSON_ITEMS);
            if (!itemsNode.isArray() || itemsNode.isEmpty()) {
                return Optional.empty();
            }
            JsonNode first = itemsNode.get(0);
            if (first == null || first.isMissingNode()) {
                return Optional.empty();
            }
            String link = first.path(JSON_LINK).asText(null);
            if (link == null || link.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(link);
        } catch (Exception e) {
            log.warn(FAIL_PARSING_LOG, query, e);
            return Optional.empty();
        }
    }

    private int defaultIfNull(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private boolean hasCredentials() {
        return properties.clientId() != null
                && !properties.clientId().isBlank()
                && properties.clientSecret() != null
                && !properties.clientSecret().isBlank();
    }
}