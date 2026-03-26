package main.givelunch.security.agent;

import java.util.Optional;
import org.springframework.security.core.Authentication;

public interface AgentAuthService {
    Optional<Authentication> authenticate(String apiKey);
}
