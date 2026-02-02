package main.givelunch.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {
    public static final String EXTERNAL_FOOD_SEARCH_CACHE = "externalFoodSearch";
    public static final String NAVER_IMAGE_CACHE = "naverImage";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache externalFoodSearchCache = new CaffeineCache(
                EXTERNAL_FOOD_SEARCH_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(2))
                        .maximumSize(500)
                        .build()
        );

        CaffeineCache naverImageCache = new CaffeineCache(
                NAVER_IMAGE_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofHours(12))
                        .maximumSize(1000)
                        .build()
        );

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(externalFoodSearchCache, naverImageCache));
        return manager;
    }
}