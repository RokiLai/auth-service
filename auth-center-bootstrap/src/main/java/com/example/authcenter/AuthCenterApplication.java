package com.example.authcenter;

import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.example.authcenter")
public class AuthCenterApplication {

	public static void main(String[] args) {
		System.out.println("Starting Spring Boot application...");
		SpringApplication application = new SpringApplication(AuthCenterApplication.class);
		application.setAddCommandLineProperties(true);
		application.run(args);
		System.out.println("Spring Boot application started.");
	}

}
