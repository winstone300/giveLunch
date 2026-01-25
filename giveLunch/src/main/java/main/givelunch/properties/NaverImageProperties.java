package main.givelunch.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.naver.image")
public record NaverImageProperties(
        String baseUrl,
        String searchPath,
        String clientId,
        String clientSecret,
        Integer display,
        Integer start,
        String sort
) {}