package com.kosmoskan.analysis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AnalysisApplication {
    public static void main(String[] args) {
           System.setProperty("java.awt.headless", "true");
        SpringApplication.run(AnalysisApplication.class, args);
    }
}
