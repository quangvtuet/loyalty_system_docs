package com.loyalty.tiering_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TieringSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(TieringSystemApplication.class, args);
    }
}
