package com.inframessaging.playground.messaging.consumer;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Consumer 상태(간단한 메모리 카운터)
 * - Kafka/Rabbit로 수신된 이벤트 수를 카운팅하여 ServiceB의 GET 요청으로 확인할 수 있게 합니다.
 */
@Component
@Getter
public class ConsumerState {
    private final AtomicInteger kafkaReceived = new AtomicInteger(0);
    private final AtomicInteger rabbitReceived = new AtomicInteger(0);

    public void incKafka() { kafkaReceived.incrementAndGet(); }
    public void incRabbit() { rabbitReceived.incrementAndGet(); }
}
