package com.inframessaging.playground.messaging.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "outbox")
public class OutboxProperties {
    private boolean enabled = true;
    private Batch batch = new Batch();
    private Schedule schedule = new Schedule();
    private Retry retry = new Retry();

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
        private int baseSeconds = 5;
        private int maxSeconds = 300;
        private double jitterRate = 0.1;
    }
}
