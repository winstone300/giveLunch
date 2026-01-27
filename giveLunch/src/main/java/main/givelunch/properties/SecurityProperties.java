package main.givelunch.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        // 설정 파일에 값이 없으면 빈 리스트([])
        @DefaultValue
        List<String> permitAllUser,

        // 설정 파일에 값이 없으면 "/admin/**"
        @DefaultValue("/admin/**")
        List<String> permitAdmin
) {}