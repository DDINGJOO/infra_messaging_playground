package com.inframessaging.playground.messaging.outbox;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.concurrent.ThreadLocalRandom;

/**
 * Outbox Processor
 * - 주기적으로 Outbox에서 전송 가능한 이벤트를 조회하여 브로커로 전송합니다.
 * - 성공 시 SENT, 실패 시 FAILED/DEAD로 상태를 갱신하며, 백오프(지수형 + 지터)에 따라 nextAttemptAt를 설정합니다.
 * - DEAD 전이 시 outbox.dead-letter.enabled=true 이면 DLQ로 즉시 복제 전송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxProcessor {

    /** Outbox JPA 리포지토리 */
    private final OutboxEventRepository repository;
    /** 배치/재시도/스케줄 설정 프로퍼티 */
    private final OutboxProperties properties;
    /** 실제 전송을 수행하는 Producer */
    private final EventProducer producer;
    /** Envelope(JSON) 처리용 */
    private final ObjectMapper objectMapper;

    /**
     * 스케줄링 간격은 outbox.schedule.delay-ms로 조절합니다(기본 1000ms).
     */
    @Scheduled(fixedDelayString = "${outbox.schedule.delay-ms:1000}")
    public void process() {
        int pageSize = Math.max(1, properties.getBatch().getSize());
        List<OutboxEventEntity> batch = repository.findProcessable(Instant.now(), PageRequest.of(0, pageSize));
        if (batch.isEmpty()) return;
        for (OutboxEventEntity e : batch) {
            try {
                Map<String, String> headers = buildHeadersSafely(e.getEnvelope());

                // 브로커 타입에 따라 전송 경로 분기
                if (e.getBrokerType() == BrokerType.KAFKA) {
                    producer.sendKafka(e.getKafkaTopic(), e.getMessageKey(), e.getEnvelope(), headers);
                } else {
                    producer.sendRabbit(e.getRabbitExchange(), e.getRabbitRoutingKey(), e.getEnvelope(), headers);
                }

                // 성공 전이
                e.setStatus(OutboxEventStatus.SENT);
                e.setSentAt(Instant.now());
                e.setUpdatedAt(Instant.now());
            } catch (Exception ex) {
                // 실패 전이 및 재시도 카운트 증가
                int attempts = e.getRetryCount() + 1;
                e.setRetryCount(attempts);
                boolean isDead = attempts >= properties.getRetry().getMaxAttempts();
                e.setStatus(isDead ? OutboxEventStatus.DEAD : OutboxEventStatus.FAILED);
                e.setLastErrorMessage(ex.getMessage());

                long backoffSec = calcBackoffSeconds(attempts);
                e.setNextAttemptAt(Instant.now().plusSeconds(backoffSec));
                e.setUpdatedAt(Instant.now());

                log.warn("Outbox send failed id={} attempts={} status={}", e.getId(), attempts, e.getStatus(), ex);

                // DEAD 전이 시 DLQ 전송 옵션 처리
                if (isDead && properties.getDeadLetter() != null && properties.getDeadLetter().isEnabled()) {
                    try {
                        Map<String, String> headers = buildHeadersSafely(e.getEnvelope());
                        Map<String, String> dlqHeaders = buildDlqHeaders(headers, e.getLastErrorMessage());
                        sendToDlq(e, dlqHeaders);
                    } catch (Exception dlqEx) {
                        log.warn("DLQ forward failed id={} reason={}", e.getId(), dlqEx.getMessage(), dlqEx);
                    }
                }
            }
        }
        // 배치 업데이트
        repository.saveAll(batch);
    }

    /**
     * Envelope JSON에서 공통 헤더 값을 추출합니다. 실패 시 안전한 기본값을 사용합니다.
     */
    private Map<String, String> buildHeadersSafely(String envelopeJson) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Event-Type", "unknown");
        headers.put("X-Event-Version", "0");
        headers.put("X-Trace-Id", "");
        headers.put("X-Occurred-At", "");
        headers.put("X-Producer-Service", "");
        if (envelopeJson == null || envelopeJson.isBlank()) return headers;
        try {
            JsonNode root = objectMapper.readTree(envelopeJson);
            JsonNode type = root.get("type");
            JsonNode version = root.get("version");
            JsonNode occurredAt = root.get("occurredAt");
            JsonNode trace = root.get("trace");
            JsonNode producer = root.get("producer");
            if (type != null) headers.put("X-Event-Type", type.asText());
            if (version != null) headers.put("X-Event-Version", version.asText());
            if (occurredAt != null) headers.put("X-Occurred-At", occurredAt.asText());
            if (trace != null && trace.get("traceId") != null) headers.put("X-Trace-Id", trace.get("traceId").asText());
            if (producer != null && producer.get("service") != null) headers.put("X-Producer-Service", producer.get("service").asText());
        } catch (Exception ignored) {
            // 파싱 실패 시 기본값 유지
        }
        return headers;
    }

    /** DLQ 전용 헤더 구성: 원본 헤더 + DLQ 이유 */
    private Map<String, String> buildDlqHeaders(Map<String, String> base, String reason) {
        Map<String, String> m = new HashMap<>(base != null ? base : Map.of());
        if (reason != null) {
            m.put("X-DLQ-Reason", reason);
        }
        m.put("X-Dead-Letter", "true");
        return m;
    }

    /** DEAD → DLQ 전송 */
    private void sendToDlq(OutboxEventEntity e, Map<String, String> headers) {
        OutboxProperties.DeadLetter dl = properties.getDeadLetter();
        if (e.getBrokerType() == BrokerType.KAFKA) {
            String dlqTopic = (e.getKafkaTopic() == null ? "" : e.getKafkaTopic()) + (dl.getKafkaSuffix() == null ? "" : dl.getKafkaSuffix());
            producer.sendKafka(dlqTopic, e.getMessageKey(), e.getEnvelope(), headers);
            log.info("[DLQ][KAFKA] forwarded id={} topic={} key={}", e.getId(), dlqTopic, e.getMessageKey());
        } else {
            String exchange = e.getRabbitExchange();
            String routingKey = (e.getRabbitRoutingKey() == null ? "" : e.getRabbitRoutingKey()) + (dl.getRabbitSuffix() == null ? "" : dl.getRabbitSuffix());
            producer.sendRabbit(exchange, routingKey, e.getEnvelope(), headers);
            log.info("[DLQ][RABBIT] forwarded id={} exchange={} routingKey={}", e.getId(), exchange, routingKey);
        }
    }

    /**
     * 지수 백오프 + 지터
     * baseSeconds * (2^(attempts-1)) 를 기반으로 하고, maxSeconds로 상한을 둡니다.
     * jitterRate가 0.1 이면 ±10% 범위에서 랜덤 지터를 적용합니다.
     */
    private long calcBackoffSeconds(int attempts) {
        int base = Math.max(1, properties.getRetry().getBackoff().getBaseSeconds());
        int max = Math.max(base, properties.getRetry().getBackoff().getMaxSeconds());
        double jitter = Math.max(0.0, properties.getRetry().getBackoff().getJitterRate());
        // pow with cap to avoid overflow
        long exp = 1L << Math.min(Math.max(attempts - 1, 0), 20); // 2^(attempts-1), capped
        double raw = base * (double) exp;
        double capped = Math.min(raw, max);
        double delta = capped * jitter;
        double min = Math.max(1.0, capped - delta);
        double maxV = capped + delta;
        long withJitter = (long) Math.round(ThreadLocalRandom.current().nextDouble(min, maxV));
        return Math.max(1, withJitter);
    }
}
