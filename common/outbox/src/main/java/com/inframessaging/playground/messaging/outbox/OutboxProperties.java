package com.inframessaging.playground.messaging.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * outbox.* 구성 프로퍼티 바인딩
 * - enabled: Processor 활성/비활성
 * - batch.size: 한 번에 가져올 처리 건수
 * - schedule.delay-ms: 폴링 주기(ms)
 * - retry.max-attempts: 실패 시 최대 재시도 횟수
 * - retry.backoff.*: 백오프 파라미터(기본/최대/지터율)
 */
@Data
@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {
    private boolean enabled = true;
    private Batch batch = new Batch();
    private Schedule schedule = new Schedule();
    private Retry retry = new Retry();
    private DeadLetter deadLetter = new DeadLetter();

    @Data
    public static class Batch { private int size = 100; }
    @Data
    public static class Schedule { private long delayMs = 1000; }
    @Data
    public static class Retry {
        private int maxAttempts = 5;
        private Backoff backoff = new Backoff();
    }
    @Data
    public static class Backoff {
        /** 백오프 기본 초(지수 증가의 베이스) */
        private int baseSeconds = 5;
        /** 백오프 최대 초(상한) */
        private int maxSeconds = 300;
        /** 지터율(랜덤 흔들림 비율, 0.1 = ±10%) */
        private double jitterRate = 0.1;
    }
    @Data
    public static class DeadLetter {
        /** DEAD 시 DLQ 전송 활성화 여부(현재 PoC는 미사용) */
        private boolean enabled = false;
        /** Kafka DLQ 토픽 suffix (예: .DLQ) */
        private String kafkaSuffix = ".DLQ";
        /** Rabbit DLQ routing suffix (예: .dlq) */
        private String rabbitSuffix = ".dlq";
    }
}
