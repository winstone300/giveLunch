package main.givelunch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan("main.givelunch.properties")
@EnableScheduling
@EnableCaching
public class GiveLunchApplication {

	public static void main(String[] args) {
		SpringApplication.run(GiveLunchApplication.class, args);
	}

}
