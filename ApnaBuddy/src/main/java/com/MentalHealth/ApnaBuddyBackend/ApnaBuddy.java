package com.MentalHealth.ApnaBuddyBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.TimeZone;

@SpringBootApplication
public class ApnaBuddy {

	public static void main(String[] args) {
		// Force the modern timezone name before the app starts
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));

		SpringApplication.run(ApnaBuddy.class, args);
	}
}