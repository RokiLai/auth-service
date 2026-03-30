package com.example.authservice;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

@EnableDiscoveryClient
@EnableDubbo
@SpringBootApplication(scanBasePackages = "com.example.authservice")
public class AuthServiceApplication {

	public static void main(String[] args) {
		System.out.println("Starting Spring Boot application...");
		SpringApplication application = new SpringApplication(AuthServiceApplication.class);
		application.setAddCommandLineProperties(true);
		application.run(args);
		System.out.println("Spring Boot application started.");
	}

}
