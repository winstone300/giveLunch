package main.givelunch.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.menu")
public record MenuProperties(List<String> defaults) {}