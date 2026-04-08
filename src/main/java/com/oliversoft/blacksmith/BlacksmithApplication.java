package com.oliversoft.blacksmith;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlacksmithApplication {

	public static void main(String[] args) {
		var application = new SpringApplication(BlacksmithApplication.class);
		application.setDefaultProperties(Map.of("spring.batch.job.enabled", "false"));
		application.run(args);
	}

}
