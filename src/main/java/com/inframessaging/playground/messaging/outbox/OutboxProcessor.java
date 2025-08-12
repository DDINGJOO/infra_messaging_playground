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

/**
 * Outbox Processor
 * - 주기적으로 Outbox에서 전송 가능한 이벤트를 조회하여 브로커로 전송합니다.
 * - 성공 시 SENT, 실패 시 FAILED/DEAD로 상태를 갱신하며, 백오프(지수형)에 따라 nextAttemptAt를 설정합니다.
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
    /** 실제 전송을 수행하는 Producer(로그 전송으로 대체된 PoC) */
    private final EventProducer producer;
    /** Envelope(JSON) 처리용(현재 PoC에서는 직접 사용 범위 제한) */
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
                // 공통 헤더(예: 타입/버전/트레이스)를 주입할 수 있습니다. 현재 PoC는 예시만 포함합니다.
                Map<String, String> headers = new HashMap<>();
                headers.put("X-Event-Type", "N/A");

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
                e.setStatus(attempts >= properties.getRetry().getMaxAttempts() ? OutboxEventStatus.DEAD : OutboxEventStatus.FAILED);
                e.setLastErrorMessage(ex.getMessage());

                // 간단한 지수 백오프(2^attempts, 상한 maxSeconds 적용)
                long backoffSec = Math.min(
                        properties.getRetry().getBackoff().getBaseSeconds() * (1L << Math.min(attempts, 10)),
                        properties.getRetry().getBackoff().getMaxSeconds()
                );
                e.setNextAttemptAt(Instant.now().plusSeconds(backoffSec));
                e.setUpdatedAt(Instant.now());

                log.warn("Outbox send failed id={} attempts={} status={}", e.getId(), attempts, e.getStatus(), ex);
            }
        }
        // 배치 업데이트
        repository.saveAll(batch);
    }
}
