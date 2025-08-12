package com.inframessaging.playground.messaging.producer;

import com.inframessaging.playground.messaging.api.BrokerType;
import java.util.Map;

/**
 * 브로커별 전송을 추상화한 Producer 인터페이스
 * - 구현체는 실제 Kafka/RabbitTemplate 등을 사용하여 전송합니다.
 * - 본 PoC에서는 LoggingEventProducer가 로그로 대체합니다.
 */
public interface EventProducer {
    /** 현재 구현이 지원/선택한 브로커 타입 */
    BrokerType brokerType();

    /** Kafka 전송: topic + key + body(JSON) + headers */
    void sendKafka(String topic, String key, String bodyJson, Map<String, String> headers);
    /** Rabbit 전송: exchange + routingKey + body(JSON) + headers */
    void sendRabbit(String exchange, String routingKey, String bodyJson, Map<String, String> headers);
}
