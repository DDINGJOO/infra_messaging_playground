package com.inframessaging.playground.messaging.producer;

import com.inframessaging.playground.messaging.api.BrokerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;

/**
 * 실제 Kafka/RabbitTemplate로 전송하는 Producer 구현체
 */
@Slf4j
@RequiredArgsConstructor
public class RealEventProducer implements EventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Override
    public BrokerType brokerType() {
        return BrokerType.KAFKA; // 의미 없음(양쪽 모두 지원), 필요 시 확장
    }

    @Override
    public void sendKafka(String topic, String key, String bodyJson, Map<String, String> headers) {
        // 간단 구현: 헤더는 생략(필요 시 KafkaHeaders로 주입)
        log.info("[RealProducer][KAFKA] send topic={} key={} headers={} body={} ", topic, key, headers, bodyJson);
        kafkaTemplate.send(topic, key, bodyJson);
    }

    @Override
    public void sendRabbit(String exchange, String routingKey, String bodyJson, Map<String, String> headers) {
        // 간단 구현: 헤더는 생략(필요 시 MessageProperties 이용)
        log.info("[RealProducer][RABBIT] send exchange={} routingKey={} headers={} body={} ", exchange, routingKey, headers, bodyJson);
        rabbitTemplate.convertAndSend(exchange, routingKey, bodyJson);
    }
}
