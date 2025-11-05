package com.example.schedulewatcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ScheduleWatcherApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScheduleWatcherApplication.class, args);
    }
}
