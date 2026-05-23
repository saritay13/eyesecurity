package com.eyesecurity.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.eyesecurity")
public class AnalyticsIngestionApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnalyticsIngestionApplication.class, args);
    }
}
