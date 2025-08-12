package com.inframessaging.playground.messaging.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inframessaging.playground.messaging.api.Envelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 데모 컨슈머
 * - demo.rabbit.queue 큐를 구독하여 Envelope(JSON)를 수신/로그합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitDemoConsumer {

    private final ObjectMapper objectMapper;
    private final ConsumerState state;

    @RabbitListener(queues = "${demo.rabbit.queue:user.profile.updated.queue}")
    public void onMessage(String message) {
        try {
            Envelope<?> env = objectMapper.readValue(message, new TypeReference<Envelope<?>>(){});
            state.incRabbit();
            log.info("[RabbitDemoConsumer] 수신 envelope: type={} version={} routing={} payload={} (rabbitReceived={})",
                    env.getType(), env.getVersion(), env.getRouting(), env.getPayload(), state.getRabbitReceived().get());
        } catch (Exception e) {
            log.warn("[RabbitDemoConsumer] 역직렬화 실패, raw={} error={}", message, e.getMessage());
        }
    }
}
