package main.givelunch;

import main.givelunch.properties.MenuProperties;
import main.givelunch.properties.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan("main.givelunch.properties")
public class GiveLunchApplication {

	public static void main(String[] args) {
		SpringApplication.run(GiveLunchApplication.class, args);
	}

}
