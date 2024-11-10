package com.luke.pingclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PingClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(PingClientApplication.class, args);
	}

}
