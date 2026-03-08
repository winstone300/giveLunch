package main.givelunch.properties;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// 기본 제공 메뉴 리스트
@Validated
@ConfigurationProperties(prefix = "app.menu")
public record MenuProperties(
        List<String> defaults,
        @Valid SuggestProperties suggest
) {
    public record SuggestProperties(
            @Min(1) int candidateFetchLimit,
            @Min(1) int resultLimit
    ) {}
}
