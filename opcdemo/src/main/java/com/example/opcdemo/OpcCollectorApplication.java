package com.example.opcdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(OpcProperties.class)
public class OpcCollectorApplication {

	public static void main(String[] args) {
		SpringApplication.run(OpcCollectorApplication.class, args);
	}
}