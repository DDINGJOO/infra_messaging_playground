package com.inframessaging.playground.messaging.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inframessaging.playground.messaging.api.Envelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 데모 컨슈머(두 번째 큐)
 * - demo.rabbit.queue2 큐를 구독하여 Envelope(JSON)를 수신/로그합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitDemoConsumer2 {

    private final ObjectMapper objectMapper;
    private final ConsumerState state;

    @RabbitListener(queues = "${demo.rabbit.queue2:user.activity.logged.queue}")
    public void onMessage(String message) {
        try {
            Envelope<?> env = objectMapper.readValue(message, new TypeReference<Envelope<?>>(){});
            state.incRabbit();
            log.info("[RabbitDemoConsumer2] 수신 envelope: type={} version={} routing={} payload={} (rabbitReceived={})",
                    env.getType(), env.getVersion(), env.getRouting(), env.getPayload(), state.getRabbitReceived().get());
        } catch (Exception e) {
            log.warn("[RabbitDemoConsumer2] 역직렬화 실패, raw={} error={}", message, e.getMessage());
        }
    }
}
