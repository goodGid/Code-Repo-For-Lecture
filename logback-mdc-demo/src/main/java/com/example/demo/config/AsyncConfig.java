package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 *
 * [핵심 포인트]
 * - @Async 사용 시 MDC가 자동으로 전파되지 않음
 * - MdcTaskDecorator를 설정하여 자동 전파 구현
 *
 * [사용 방법]
 * 1. 이 설정을 추가
 * 2. Service 메서드에 @Async("mdcTaskExecutor") 추가
 * 3. MDC 값이 자동으로 비동기 스레드에 전파됨
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "mdcTaskExecutor")
    public Executor mdcTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 스레드 풀 설정
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("MDC-Async-");

        // 핵심: MdcTaskDecorator 설정
        // 이 설정으로 @Async 사용 시 MDC가 자동으로 전파됨
        executor.setTaskDecorator(new MdcTaskDecorator());

        executor.initialize();
        return executor;
    }
}
