package com.oliversoft.blacksmith;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class BlacksmithApplication {

	public static void main(String[] args) {
		var application = new SpringApplication(BlacksmithApplication.class);
		application.setDefaultProperties(Map.of("spring.batch.job.enabled", "false"));
		application.run(args);
	}

}
