package com.highlight.highlight_backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 스케줄링 및 비동기 처리 설정
 * 
 * @author 전우선
 * @since 2025.08.18
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
    
    /**
     * TaskScheduler 빈 설정
     * 경매 스케줄링을 위한 스케줄러를 제공합니다.
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("auction-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}