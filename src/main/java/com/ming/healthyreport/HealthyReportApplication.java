package com.ming.healthyreport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaRepositories
@EnableSpringDataWebSupport
@EnableScheduling
public class HealthyReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(HealthyReportApplication.class, args);
    }

}
