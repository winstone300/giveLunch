package main.givelunch.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("app.rank")
public record RankProperties(
        @DefaultValue("24") Duration retention
) {
}
