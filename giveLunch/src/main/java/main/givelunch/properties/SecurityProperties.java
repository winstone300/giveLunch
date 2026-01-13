package main.givelunch.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private List<String> permitAll = new ArrayList<>();

    public List<String> getPermitAll() { return permitAll; }
    public void setPermitAll(List<String> permitAll) { this.permitAll = permitAll; }
}
