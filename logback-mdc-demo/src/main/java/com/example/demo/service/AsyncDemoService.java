package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * @Async와 MDC 연동 예시
 *
 * [핵심 개념]
 * - @Async만 사용하면 MDC가 전파되지 않음
 * - @Async("mdcTaskExecutor") 사용 시 MdcTaskDecorator에 의해 MDC 자동 전파
 *
 * [비교]
 * 1. sendEmailWithoutMdc() - MDC 전파 안됨
 * 2. sendEmailWithMdc() - MDC 전파됨 (MdcTaskDecorator 사용)
 */
@Slf4j
@Service
public class AsyncDemoService {

    /**
     * MDC 전파 안되는 비동기 메서드
     * - 기본 TaskExecutor 사용
     * - MDC 값이 비어있음
     */
    @Async
    public CompletableFuture<String> sendEmailWithoutMdc(String email) {
        log.info("[Async-NoMDC] 이메일 발송 시작 - to: {}", email);
        log.info("[Async-NoMDC] 현재 requestId: {} (비어있을 것임)", MDC.get("requestId"));

        // 이메일 발송 로직 시뮬레이션
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[Async-NoMDC] 이메일 발송 완료 - to: {}", email);
        return CompletableFuture.completedFuture("Email sent (without MDC): " + email);
    }

    /**
     * MDC 전파되는 비동기 메서드
     * - mdcTaskExecutor 사용 (MdcTaskDecorator 적용됨)
     * - MDC 값이 유지됨
     */
    @Async("mdcTaskExecutor")
    public CompletableFuture<String> sendEmailWithMdc(String email) {
        log.info("[Async-MDC] 이메일 발송 시작 - to: {}", email);
        log.info("[Async-MDC] 현재 requestId: {} (값이 있을 것임!)", MDC.get("requestId"));

        // 이메일 발송 로직 시뮬레이션
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("[Async-MDC] 이메일 발송 완료 - to: {}", email);
        return CompletableFuture.completedFuture("Email sent (with MDC): " + email);
    }

    /**
     * 여러 비동기 작업 연쇄 호출 예시
     * - MDC가 모든 비동기 작업에 전파됨
     */
    @Async("mdcTaskExecutor")
    public CompletableFuture<String> processNotification(String userId, String message) {
        log.info("[Async-MDC] 알림 처리 시작 - userId: {}, message: {}", userId, message);

        // 알림 로직
        saveNotification(userId, message);
        sendPushNotification(userId, message);

        log.info("[Async-MDC] 알림 처리 완료 - userId: {}", userId);
        return CompletableFuture.completedFuture("Notification processed for: " + userId);
    }

    private void saveNotification(String userId, String message) {
        log.debug("[Async-MDC] DB에 알림 저장 - userId: {}", userId);
        // DB 저장 로직...
    }

    private void sendPushNotification(String userId, String message) {
        log.debug("[Async-MDC] 푸시 알림 발송 - userId: {}", userId);
        // 푸시 발송 로직...
    }
}
