package com.beautytextile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BeautyTextileApplication {
    public static void main(String[] args) {
        SpringApplication.run(BeautyTextileApplication.class, args);
    }
}
