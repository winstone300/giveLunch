package main.givelunch.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agent-auth")
public record AgentAuthProperties(
        String apiKey
) {
}
