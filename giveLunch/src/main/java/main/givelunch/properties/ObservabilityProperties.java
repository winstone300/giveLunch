package main.givelunch.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.observability")
public record ObservabilityProperties(
        @DefaultValue("true")
        boolean enabled,
        @DefaultValue("X-LoadTest-Run-Id")
        String runIdHeader,
        @DefaultValue("X-LoadTest-Scenario")
        String scenarioHeader,
        Sql sql
) {
    public record Sql(
            @DefaultValue("true")
            boolean enabled,
            @DefaultValue("200")
            long slowQueryMs
    ) {
    }
}
