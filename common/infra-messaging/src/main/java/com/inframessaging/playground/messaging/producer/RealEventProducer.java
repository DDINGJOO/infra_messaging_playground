package com.inframessaging.playground.messaging.producer;

import com.inframessaging.playground.messaging.api.BrokerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;
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
        // Kafka 헤더 주입: ProducerRecord + RecordHeaders 사용
        RecordHeaders recordHeaders = new RecordHeaders();
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    recordHeaders.add(k, v.getBytes(StandardCharsets.UTF_8));
                }
            });
        }
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, null, key, bodyJson, recordHeaders);
        log.info("[Producer][KAFKA] send topic={} key={} headers={} body={} ", topic, key, headers, bodyJson);
        kafkaTemplate.send(record);
    }

    @Override
    public void sendRabbit(String exchange, String routingKey, String bodyJson, Map<String, String> headers) {
        // Rabbit 헤더 주입: MessageProperties 사용
        MessageProperties props = new MessageProperties();
        props.setContentType("application/json");
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null && v != null) {
                    props.setHeader(k, v);
                }
            });
        }
        Message message = new Message(bodyJson.getBytes(StandardCharsets.UTF_8), props);
        log.info("[Producer][RABBIT] send exchange={} routingKey={} headers={} body={} ", exchange, routingKey, headers, bodyJson);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
