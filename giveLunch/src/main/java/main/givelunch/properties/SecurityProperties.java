package main.givelunch.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private final List<String> permitAll;

    public List<String> getPermitAll() { return permitAll; }
    public SecurityProperties(List<String> permitAll) {
        this.permitAll = (permitAll != null) ? permitAll : new ArrayList<>();
    }
}
