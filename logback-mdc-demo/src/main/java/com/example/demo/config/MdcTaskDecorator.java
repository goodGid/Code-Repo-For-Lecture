package com.example.demo.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * MDC 컨텍스트를 비동기 스레드로 전파하는 TaskDecorator
 *
 * [사용 목적]
 * - @Async 어노테이션 사용 시 MDC 값이 자동으로 전파되지 않음
 * - TaskDecorator를 사용하면 자동으로 MDC 값을 복사/전파 가능
 *
 * [동작 원리]
 * 1. 원본 Runnable이 제출될 때 현재 스레드의 MDC 복사
 * 2. 새 Runnable로 감싸서 반환
 * 3. 새 스레드에서 실행 시 복사한 MDC 값 설정
 * 4. 실행 완료 후 MDC 정리
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 1. 현재 스레드(요청 스레드)의 MDC 복사
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        // 2. 새로운 Runnable로 감싸서 반환
        return () -> {
            try {
                // 3. 비동기 스레드에서 실행 전에 MDC 설정
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }

                // 4. 원본 작업 실행
                runnable.run();

            } finally {
                // 5. 작업 완료 후 MDC 정리
                MDC.clear();
            }
        };
    }
}
