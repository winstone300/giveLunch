package main.givelunch.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

// 기본 제공 메뉴 리스트
@ConfigurationProperties(prefix = "app.menu")
public record MenuProperties(List<String> defaults) {}