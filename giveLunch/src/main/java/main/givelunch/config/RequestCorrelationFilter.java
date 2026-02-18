package main.givelunch.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import main.givelunch.properties.ObservabilityProperties;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private final ObservabilityProperties properties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String runIdHeader = properties.runIdHeader();
        String scenarioHeader = properties.scenarioHeader();
        String runId = normalizeOrDefault(request.getHeader(runIdHeader), UUID.randomUUID().toString());
        String scenario = normalizeOrDefault(request.getHeader(scenarioHeader), "unknown");

        MDC.put("runId", runId);
        MDC.put("scenario", scenario);
        response.setHeader(runIdHeader, runId);
        response.setHeader(scenarioHeader, scenario);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("runId");
            MDC.remove("scenario");
        }
    }

    private String normalizeOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
