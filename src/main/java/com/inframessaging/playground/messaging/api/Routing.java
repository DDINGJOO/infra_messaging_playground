package com.inframessaging.playground.messaging.api;

import lombok.*;

/**
 * 라우팅 메타데이터(브로커 공통)
 * - broker: 사용 브로커(KAFKA/RABBIT)
 * - kafka: Kafka 전용 라우팅 정보(topic, key)
 * - rabbit: RabbitMQ 전용 라우팅 정보(exchange, routingKey)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Routing {
    /** 사용 브로커 타입 */
    BrokerType broker;
    /** Kafka 전용 라우팅 정보 */
    Kafka kafka;
    /** RabbitMQ 전용 라우팅 정보 */
    Rabbit rabbit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Kafka {
        /** 발행할 토픽명 */
        String topic;
        /** 메시지 키(파티션 키) */
        String key;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rabbit {
        /** 발행할 익스체인지명 */
        String exchange;
        /** 라우팅 키 */
        String routingKey;
    }
}
