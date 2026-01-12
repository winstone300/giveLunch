package main.givelunch;

import main.givelunch.config.MenuProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MenuProperties.class)
public class GiveLunchApplication {

	public static void main(String[] args) {
		SpringApplication.run(GiveLunchApplication.class, args);
	}

}
