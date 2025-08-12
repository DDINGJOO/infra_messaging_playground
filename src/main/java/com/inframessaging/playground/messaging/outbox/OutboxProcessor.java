package com.inframessaging.playground.messaging.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inframessaging.playground.messaging.api.BrokerType;
import com.inframessaging.playground.messaging.producer.EventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxProcessor {

    private final OutboxEventRepository repository;
    private final OutboxProperties properties;
    private final EventProducer producer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.schedule.delay-ms:1000}")
    public void process() {
        int pageSize = Math.max(1, properties.getBatch().getSize());
        List<OutboxEventEntity> batch = repository.findProcessable(Instant.now(), PageRequest.of(0, pageSize));
        if (batch.isEmpty()) return;
        for (OutboxEventEntity e : batch) {
            try {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-Event-Type", "N/A");
                if (e.getBrokerType() == BrokerType.KAFKA) {
                    producer.sendKafka(e.getKafkaTopic(), e.getMessageKey(), e.getEnvelope(), headers);
                } else {
                    producer.sendRabbit(e.getRabbitExchange(), e.getRabbitRoutingKey(), e.getEnvelope(), headers);
                }
                e.setStatus(OutboxEventStatus.SENT);
                e.setSentAt(Instant.now());
                e.setUpdatedAt(Instant.now());
            } catch (Exception ex) {
                int attempts = e.getRetryCount() + 1;
                e.setRetryCount(attempts);
                e.setStatus(attempts >= properties.getRetry().getMaxAttempts() ? OutboxEventStatus.DEAD : OutboxEventStatus.FAILED);
                e.setLastErrorMessage(ex.getMessage());
                long backoffSec = Math.min(properties.getRetry().getBackoff().getBaseSeconds() * (1L << Math.min(attempts, 10)), properties.getRetry().getBackoff().getMaxSeconds());
                e.setNextAttemptAt(Instant.now().plusSeconds(backoffSec));
                e.setUpdatedAt(Instant.now());
                log.warn("Outbox send failed id={} attempts={} status={}", e.getId(), attempts, e.getStatus(), ex);
            }
        }
        repository.saveAll(batch);
    }
}
