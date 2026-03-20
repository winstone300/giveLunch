package main.givelunch.security.agent;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import main.givelunch.properties.AgentAuthProperties;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FixedApiKeyAgentAuthService implements AgentAuthService {
    public static final String AGENT_API_ROLE = "ROLE_AGENT_API";
    private static final String PRINCIPAL = "agent-api";

    private final AgentAuthProperties properties;

    @Override
    public Optional<Authentication> authenticate(String apiKey) {
        String configuredApiKey = properties.apiKey();
        if (configuredApiKey == null || configuredApiKey.isBlank()) {
            return Optional.empty();
        }
        if (apiKey == null || apiKey.isBlank()) {
            return Optional.empty();
        }
        if (!MessageDigest.isEqual(
                configuredApiKey.getBytes(StandardCharsets.UTF_8),
                apiKey.getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }
        return Optional.of(new UsernamePasswordAuthenticationToken(
                PRINCIPAL,
                "N/A",
                List.of(new SimpleGrantedAuthority(AGENT_API_ROLE))));
    }
}
