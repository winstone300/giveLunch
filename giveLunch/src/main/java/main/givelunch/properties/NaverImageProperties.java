package main.givelunch.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.naver.image")
public record NaverImageProperties(
        String baseUrl,
        String searchPath,
        String clientId,
        String clientSecret,
        Integer display,    // 1번에 표시할 결과 개수
        Integer start,  // 검색 시작 위치
        String sort // 정렬 방법(sim: 정확도순 , date: 날짜순)
) {}