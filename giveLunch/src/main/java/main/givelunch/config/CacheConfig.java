package main.givelunch.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import main.givelunch.properties.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {
    public static final String EXTERNAL_FOOD_SEARCH_CACHE = "externalFoodSearch";
    public static final String NAVER_IMAGE_CACHE = "naverImage";

    @Bean
    public CacheManager cacheManager(CacheProperties cacheProperties) {
        CaffeineCache externalFoodSearchCache = new CaffeineCache(
                EXTERNAL_FOOD_SEARCH_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheProperties.getExternalFoodSearch().getExpireAfterWrite())
                        .maximumSize(cacheProperties.getExternalFoodSearch().getMaximumSize())
                        .build()
        );

        CaffeineCache naverImageCache = new CaffeineCache(
                NAVER_IMAGE_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(cacheProperties.getNaverImage().getExpireAfterWrite())
                        .maximumSize(cacheProperties.getNaverImage().getMaximumSize())
                        .build()
        );

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(externalFoodSearchCache, naverImageCache));
        return manager;
    }
}