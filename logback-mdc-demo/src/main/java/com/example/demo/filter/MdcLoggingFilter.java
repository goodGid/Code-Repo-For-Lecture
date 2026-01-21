package com.example.demo.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * MDC (Mapped Diagnostic Context) 설정 필터
 *
 * [핵심 개념]
 * 1. MDC는 ThreadLocal 기반으로 동작
 * 2. 각 HTTP 요청은 별도의 스레드에서 처리됨
 * 3. 따라서 요청별로 독립적인 컨텍스트 정보 유지 가능
 *
 * [사용 목적]
 * - 분산 시스템에서 요청 추적 (Distributed Tracing)
 * - 로그 분석 시 특정 요청의 전체 흐름 파악
 * - 문제 발생 시 관련 로그만 필터링하여 조회
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // 가장 먼저 실행되도록 설정
public class MdcLoggingFilter extends OncePerRequestFilter {

    // MDC Key 상수 정의
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            // ========================================
            // 1. MDC에 컨텍스트 정보 설정
            // ========================================

            // 요청 ID 설정 (헤더에 있으면 사용, 없으면 새로 생성)
            // X-Request-Id 헤더는 API Gateway나 Load Balancer에서 설정해주는 경우가 많음
            String requestId = request.getHeader("X-Request-Id");
            if (requestId == null || requestId.isEmpty()) {
                requestId = generateRequestId();
            }
            MDC.put(REQUEST_ID, requestId);

            // 사용자 ID 설정 (실제로는 인증 정보에서 가져옴)
            // 예시: SecurityContextHolder에서 가져오기
            String userId = request.getHeader("X-User-Id");
            if (userId == null || userId.isEmpty()) {
                userId = "anonymous";
            }
            MDC.put(USER_ID, userId);

            // 응답 헤더에도 Request ID 추가 (클라이언트가 추적에 사용 가능)
            response.setHeader("X-Request-Id", requestId);

            // ========================================
            // 2. 요청 시작 로그
            // ========================================
            log.info(">>> 요청 시작: {} {}", request.getMethod(), request.getRequestURI());

            // ========================================
            // 3. 다음 필터 또는 컨트롤러 실행
            // ========================================
            filterChain.doFilter(request, response);

            // ========================================
            // 4. 요청 완료 로그
            // ========================================
            log.info("<<< 요청 완료: {} {} - Status: {}",
                    request.getMethod(), request.getRequestURI(), response.getStatus());

        } finally {
            // ========================================
            // 5. MDC 정리 (매우 중요!)
            // ========================================
            // ThreadPool을 사용하는 경우, 스레드가 재사용되므로
            // MDC를 정리하지 않으면 이전 요청의 정보가 남아있을 수 있음
            MDC.clear();
        }
    }

    /**
     * 고유한 요청 ID 생성
     * 실무에서는 UUID 외에도 다양한 방식 사용:
     * - Snowflake ID
     * - ULID
     * - 타임스탬프 + 랜덤값 조합
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
