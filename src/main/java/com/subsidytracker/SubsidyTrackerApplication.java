package com.subsidytracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SubsidyTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SubsidyTrackerApplication.class, args);
	}
}