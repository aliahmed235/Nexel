package com.aliahmed.Vercel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VercelApplication {

	public static void main(String[] args) {
		SpringApplication.run(VercelApplication.class, args);
	}

}
