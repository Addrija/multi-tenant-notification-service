package com.notification.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MultiTenantNotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultiTenantNotificationServiceApplication.class, args);
	}

}
