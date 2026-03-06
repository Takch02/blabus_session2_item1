package com.highlight.highlight_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class HighlightBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighlightBackendApplication.class, args);
    }

}
