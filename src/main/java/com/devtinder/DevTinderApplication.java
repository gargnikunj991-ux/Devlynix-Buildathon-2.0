package com.devtinder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DevTinderApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevTinderApplication.class, args);
    }
}
