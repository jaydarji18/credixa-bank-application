package com.credixa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
@EnableScheduling
@EnableAsync
public class CredixaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CredixaBackendApplication.class, args);
	}

}
