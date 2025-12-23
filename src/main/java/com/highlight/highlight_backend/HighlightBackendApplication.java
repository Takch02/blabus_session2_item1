package com.highlight.highlight_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class HighlightBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(HighlightBackendApplication.class, args);
    }

}
