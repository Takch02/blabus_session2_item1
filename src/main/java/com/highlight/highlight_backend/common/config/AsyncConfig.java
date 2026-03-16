package com.highlight.highlight_backend.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor") // 기본 taskExecutor를 덮어씌웁니다.
    public Executor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);    // 기본 일꾼 10명
        executor.setMaxPoolSize(15);     // 바빠도 최대 30명까지만 고용
        executor.setQueueCapacity(100);  // 일꾼이 꽉 차면 100개까지만 대기열에 세움
        executor.setThreadNamePrefix("Async-Worker-"); // 이름도 예쁘게 변경
        
        // 큐(100개)마저 꽉 차면 어떻게 할 것인가? (버리지 않고, 이벤트를 발생시킨 톰캣 스레드가 직접 처리하게 만듦)
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}