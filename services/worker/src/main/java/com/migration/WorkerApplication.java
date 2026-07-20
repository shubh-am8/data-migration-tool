package com.migration;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = {"com.migration.jobs", "com.migration.connectors", "com.migration.queue"})
public class WorkerApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(WorkerApplication.class, args);
    }
}
