package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MDC 동작 예시를 보여주는 서비스
 *
 * [핵심 포인트]
 * 1. Controller에서 설정한 MDC 값이 Service까지 유지됨 (같은 스레드)
 * 2. 비동기 처리 시 MDC 값이 자동으로 전파되지 않음 (다른 스레드)
 * 3. 비동기 처리에서 MDC를 유지하려면 수동으로 복사해야 함
 */
@Slf4j
@Service
public class OrderService {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * 주문 처리 (동기)
     * - 같은 스레드에서 실행되므로 MDC 값 유지됨
     */
    public String processOrder(String orderId) {
        log.info("[OrderService] 주문 처리 시작 - orderId: {}", orderId);

        // 비즈니스 로직 수행
        validateOrder(orderId);
        calculatePrice(orderId);
        saveOrder(orderId);

        log.info("[OrderService] 주문 처리 완료 - orderId: {}", orderId);
        return "Order processed: " + orderId;
    }

    private void validateOrder(String orderId) {
        log.debug("[OrderService] 주문 유효성 검사 - orderId: {}", orderId);
        // 유효성 검사 로직...
    }

    private void calculatePrice(String orderId) {
        log.debug("[OrderService] 가격 계산 - orderId: {}", orderId);
        // 가격 계산 로직...
    }

    private void saveOrder(String orderId) {
        log.debug("[OrderService] 주문 저장 - orderId: {}", orderId);
        // DB 저장 로직...
    }

    /**
     * 비동기 주문 처리 - MDC 전파 안됨 (문제 상황)
     *
     * [문제점]
     * - 새로운 스레드에서 실행되므로 MDC 값이 비어있음
     * - 로그에 requestId, userId 등이 출력되지 않음
     */
    public CompletableFuture<String> processOrderAsync(String orderId) {
        log.info("[OrderService] 비동기 주문 처리 요청 - orderId: {}", orderId);

        return CompletableFuture.supplyAsync(() -> {
            // 이 코드는 다른 스레드에서 실행됨 -> MDC 값 없음!
            log.info("[OrderService-Async] 비동기 처리 중 - orderId: {} (MDC 값 확인해보세요!)", orderId);
            log.info("[OrderService-Async] 현재 MDC requestId: {}", MDC.get("requestId")); // null
            return "Async order processed: " + orderId;
        }, executor);
    }

    /**
     * 비동기 주문 처리 - MDC 전파됨 (해결 방법)
     *
     * [해결 방법]
     * 1. 비동기 작업 시작 전에 MDC.getCopyOfContextMap()으로 현재 MDC 복사
     * 2. 비동기 작업 내에서 MDC.setContextMap()으로 복사한 값 설정
     * 3. 작업 완료 후 MDC.clear()로 정리
     */
    public CompletableFuture<String> processOrderAsyncWithMdc(String orderId) {
        log.info("[OrderService] MDC 전파 비동기 주문 처리 요청 - orderId: {}", orderId);

        // 현재 스레드의 MDC 복사
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 복사한 MDC 값을 새 스레드에 설정
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }

                log.info("[OrderService-Async-MDC] 비동기 처리 중 - orderId: {} (MDC 값 유지됨!)", orderId);
                log.info("[OrderService-Async-MDC] 현재 MDC requestId: {}", MDC.get("requestId"));

                // 비즈니스 로직 수행
                Thread.sleep(100); // 처리 시뮬레이션

                return "Async order with MDC processed: " + orderId;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                // 중요: 스레드 풀 사용 시 반드시 MDC 정리
                MDC.clear();
            }
        }, executor);
    }

    /**
     * MDC에 추가 정보 설정하는 예시
     * - 특정 비즈니스 로직에서 추가 컨텍스트가 필요한 경우
     */
    public String processOrderWithExtraContext(String orderId, String productCategory) {
        // 기존 MDC에 추가 정보 설정
        MDC.put("orderId", orderId);
        MDC.put("productCategory", productCategory);

        try {
            log.info("[OrderService] 추가 컨텍스트와 함께 주문 처리 - category: {}", productCategory);
            // 비즈니스 로직...
            return "Order processed with extra context: " + orderId;
        } finally {
            // 추가한 MDC 값만 제거 (다른 MDC 값은 유지)
            MDC.remove("orderId");
            MDC.remove("productCategory");
        }
    }
}
