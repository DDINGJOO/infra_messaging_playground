package com.inframessaging.playground.messaging.consumer;

import com.inframessaging.playground.messaging.api.Envelope;
import com.inframessaging.playground.messaging.deser.EnvelopeDeserializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka 데모 컨슈머
 * - demo.kafka.topic 토픽을 구독하여 Envelope(JSON)를 수신/로그합니다.
 * - payload는 등록된 타입/버전에 따라 구체 클래스로 역직렬화 시도하며, 미등록 시 Map으로 처리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDemoConsumer {

    private final EnvelopeDeserializer envelopeDeserializer;
    private final ConsumerState state;

    @KafkaListener(topics = {"${demo.kafka.topic:user.profile.updated.v1}", "${demo.kafka.topic2:user.activity.logged.v1}"}, groupId = "infra-messaging-demo")
    public void onMessage(String message) {
        try {
            Envelope<?> env = envelopeDeserializer.deserialize(message);
            state.incKafka();
            log.info("[KafkaDemoConsumer] 수신 envelope: type={} version={} routing={} payload={} (kafkaReceived={})",
                    env != null ? env.getType() : "null",
                    env != null ? env.getVersion() : -1,
                    env != null ? env.getRouting() : null,
                    env != null ? env.getPayload() : null,
                    state.getKafkaReceived().get());
        } catch (Exception e) {
            log.warn("[KafkaDemoConsumer] 역직렬화 실패, raw={} error={}", message, e.getMessage());
        }
    }
}
