package com.inframessaging.playground.messaging.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inframessaging.playground.messaging.api.Envelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 데모 컨슈머
 * - messaging.type=KAFKA 일 때 활성화됩니다.
 * - demo.kafka.topic 토픽을 구독하여 Envelope(JSON)를 수신/로그합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "messaging", name = "type", havingValue = "KAFKA")
public class KafkaDemoConsumer {

    private final ObjectMapper objectMapper;
    private final ConsumerState state;

    @KafkaListener(topics = "${demo.kafka.topic:user.profile.updated.v1}", groupId = "infra-messaging-demo")
    public void onMessage(String message) {
        try {
            Envelope<?> env = objectMapper.readValue(message, new TypeReference<Envelope<?>>(){});
            state.incKafka();
            log.info("[KafkaDemoConsumer] 수신 envelope: type={} version={} routing={} payload={} (kafkaReceived={})",
                    env.getType(), env.getVersion(), env.getRouting(), env.getPayload(), state.getKafkaReceived().get());
        } catch (Exception e) {
            log.warn("[KafkaDemoConsumer] 역직렬화 실패, raw={} error={}", message, e.getMessage());
        }
    }
}
