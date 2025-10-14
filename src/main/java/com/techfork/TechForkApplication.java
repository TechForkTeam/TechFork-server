package com.techfork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class TechForkApplication {

	public static void main(String[] args) {
		SpringApplication.run(TechForkApplication.class, args);
	}

}
