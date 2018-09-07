package com.avp.resource.reload.resourcereloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ResourceReloaderApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResourceReloaderApplication.class, args);
	}
}
