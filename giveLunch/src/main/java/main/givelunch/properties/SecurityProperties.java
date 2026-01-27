package main.givelunch.properties;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
    private final List<String> permitAllUser;
    private final List<String> permitAdmin;

    public List<String> getPermitAllUser() { return permitAllUser; }
    public List<String> getPermitAdmin() { return permitAdmin; }

    public SecurityProperties(List<String> permitAllUser, List<String> permitAdmin) {
        this.permitAllUser = (permitAllUser != null) ? permitAllUser : new ArrayList<>();
        this.permitAdmin = (permitAdmin != null) ? permitAdmin : new ArrayList<>();
    }
}
