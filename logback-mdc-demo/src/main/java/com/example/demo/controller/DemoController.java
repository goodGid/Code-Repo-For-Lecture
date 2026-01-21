package com.example.demo.controller;

import com.example.demo.service.AsyncDemoService;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Logback + MDC 데모 컨트롤러
 *
 * [테스트 방법]
 * 1. 기본 요청 (MDC 값 자동 설정):
 *    curl http://localhost:8080/api/hello
 *
 * 2. 커스텀 헤더로 요청:
 *    curl -H "X-Request-Id: my-custom-id" -H "X-User-Id: user123" http://localhost:8080/api/hello
 *
 * 3. 주문 처리 (동기):
 *    curl http://localhost:8080/api/orders/12345
 *
 * 4. 비동기 처리 (MDC 전파 문제 확인):
 *    curl http://localhost:8080/api/orders/12345/async
 *
 * 5. 비동기 처리 (MDC 전파 해결):
 *    curl http://localhost:8080/api/orders/12345/async-mdc
 *
 * 6. @Async MDC 전파 비교:
 *    curl http://localhost:8080/api/email/test@example.com/without-mdc
 *    curl http://localhost:8080/api/email/test@example.com/with-mdc
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DemoController {

    private final OrderService orderService;
    private final AsyncDemoService asyncDemoService;

    /**
     * 기본 엔드포인트 - MDC 동작 확인
     */
    @GetMapping("/hello")
    public Map<String, String> hello() {
        log.info("[Controller] /hello 요청 처리 시작");

        // MDC에서 현재 값 조회하여 응답에 포함
        Map<String, String> response = new HashMap<>();
        response.put("message", "Hello! Check the console log for MDC values.");
        response.put("requestId", MDC.get("requestId"));
        response.put("userId", MDC.get("userId"));
        response.put("clientIp", MDC.get("clientIp"));

        log.info("[Controller] /hello 요청 처리 완료");
        return response;
    }

    /**
     * 다양한 로그 레벨 테스트
     */
    @GetMapping("/log-levels")
    public String logLevels() {
        log.trace("TRACE 레벨 로그 - 가장 상세한 디버깅 정보");
        log.debug("DEBUG 레벨 로그 - 개발 시 디버깅 정보");
        log.info("INFO 레벨 로그 - 일반적인 정보성 메시지");
        log.warn("WARN 레벨 로그 - 잠재적 문제 경고");
        log.error("ERROR 레벨 로그 - 에러 발생");

        return "Check console for different log levels!";
    }

    /**
     * 예외 발생 시 로깅 예시
     */
    @GetMapping("/error-demo")
    public String errorDemo() {
        try {
            log.info("[Controller] 에러 데모 시작");
            throw new RuntimeException("의도적인 예외 발생!");
        } catch (Exception e) {
            // 예외 로깅 시 MDC 정보가 함께 출력됨
            // -> 어떤 요청에서 에러가 발생했는지 추적 가능
            log.error("[Controller] 예외 발생! requestId로 관련 로그 추적 가능", e);
            return "Error occurred! Check logs with requestId: " + MDC.get("requestId");
        }
    }

    /**
     * 주문 처리 - 동기 방식
     * Controller -> Service 로 MDC 전파 확인
     */
    @GetMapping("/orders/{orderId}")
    public String processOrder(@PathVariable String orderId) {
        log.info("[Controller] 주문 처리 요청 - orderId: {}", orderId);
        String result = orderService.processOrder(orderId);
        log.info("[Controller] 주문 처리 응답 - result: {}", result);
        return result;
    }

    /**
     * 비동기 주문 처리 - MDC 전파 안됨 (문제 상황)
     */
    @GetMapping("/orders/{orderId}/async")
    public CompletableFuture<String> processOrderAsync(@PathVariable String orderId) {
        log.info("[Controller] 비동기 주문 처리 요청 (MDC 전파 X) - orderId: {}", orderId);
        return orderService.processOrderAsync(orderId);
    }

    /**
     * 비동기 주문 처리 - MDC 전파됨 (해결 방법)
     */
    @GetMapping("/orders/{orderId}/async-mdc")
    public CompletableFuture<String> processOrderAsyncWithMdc(@PathVariable String orderId) {
        log.info("[Controller] 비동기 주문 처리 요청 (MDC 전파 O) - orderId: {}", orderId);
        return orderService.processOrderAsyncWithMdc(orderId);
    }

    /**
     * MDC 수동 설정 예시
     * - 특정 상황에서 MDC 값을 수동으로 추가/변경할 수 있음
     */
    @PostMapping("/orders")
    public String createOrder(@RequestParam String productCategory) {
        log.info("[Controller] 주문 생성 요청 - category: {}", productCategory);

        // 비즈니스 로직에서 추가 컨텍스트 설정
        String orderId = "ORD-" + System.currentTimeMillis();
        String result = orderService.processOrderWithExtraContext(orderId, productCategory);

        return result;
    }

    // =====================================================
    // @Async + MDC 연동 테스트 엔드포인트
    // =====================================================

    /**
     * @Async 기본 사용 - MDC 전파 안됨
     */
    @GetMapping("/email/{email}/without-mdc")
    public CompletableFuture<String> sendEmailWithoutMdc(@PathVariable String email) {
        log.info("[Controller] 이메일 발송 요청 (MDC 전파 X) - to: {}", email);
        return asyncDemoService.sendEmailWithoutMdc(email);
    }

    /**
     * @Async + mdcTaskExecutor 사용 - MDC 전파됨
     */
    @GetMapping("/email/{email}/with-mdc")
    public CompletableFuture<String> sendEmailWithMdc(@PathVariable String email) {
        log.info("[Controller] 이메일 발송 요청 (MDC 전파 O) - to: {}", email);
        return asyncDemoService.sendEmailWithMdc(email);
    }

    /**
     * 알림 처리 - MDC 전파 + 여러 단계 호출
     */
    @PostMapping("/notification")
    public CompletableFuture<String> processNotification(
            @RequestParam String userId,
            @RequestParam String message) {
        log.info("[Controller] 알림 처리 요청 - userId: {}", userId);
        return asyncDemoService.processNotification(userId, message);
    }
}
