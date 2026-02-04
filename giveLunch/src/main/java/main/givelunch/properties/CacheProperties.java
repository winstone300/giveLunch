package main.givelunch.properties;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache")
@Setter
@Getter
public class CacheProperties {
    private CacheSpec externalFoodSearch = new CacheSpec();
    private CacheSpec naverImage = new CacheSpec();

    @Getter
    @Setter
    public static class CacheSpec {
        private Duration expireAfterWrite = Duration.ofHours(1);
        private long maximumSize = 1000;
    }
}