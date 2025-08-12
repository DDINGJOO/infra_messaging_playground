package com.inframessaging.playground.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inframessaging.playground.messaging.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.UUID;

/**
 * DomainEventPublisher 구현체.
 * - 도메인 이벤트를 Envelope로 래핑하여 Outbox에 적재합니다.
 * - Envelope 직렬화 실패 시에도 FAILED 상태로 Outbox에 저장하여 운영 가시성을 확보합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventPublisherImpl implements DomainEventPublisher {

    /** Outbox 저장소(JPA) */
    private final OutboxEventRepository repository;
    /** Envelope 직렬화(Jackson) */
    private final ObjectMapper objectMapper;

    /**
     * 라우팅 옵션이 없을 경우 기본 옵션으로 위임 호출합니다.
     */
    @Override
    public void publish(CustomEvent event) {
        publish(event, RoutingOptions.builder().build());
    }

    /**
     * 이벤트를 Envelope로 감싸 JSON으로 직렬화한 뒤, Outbox에 PENDING(또는 FAILED)으로 적재합니다.
     */
    @Override
    public void publish(CustomEvent event, RoutingOptions opts) {
        Envelope<CustomEvent> envelope = buildEnvelope(event, opts);
        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            // 직렬화 실패 시에도 Outbox에 FAILED로 저장하여 유실을 방지하고 원인(lastErrorMessage)을 남깁니다.
            log.error("Envelope serialization failed", e);
            persist(event, opts, null, OutboxEventStatus.FAILED, e.getMessage());
            return;
        }
        // 정상 직렬화 시 PENDING으로 저장 → Processor가 전송 후 SENT로 전이합니다.
        persist(event, opts, json, OutboxEventStatus.PENDING, null);
    }

    /**
     * Envelope 구성: 프로듀서/트레이스/라우팅 메타를 포함하여 표준 래퍼를 만듭니다.
     */
    private Envelope<CustomEvent> buildEnvelope(CustomEvent event, RoutingOptions opts) {
        String host = "unknown";
        try { host = InetAddress.getLocalHost().getHostName(); } catch (UnknownHostException ignored) {}
        ProducerInfo producer = ProducerInfo.builder().service("infra-messaging-playground").host(host).env("local").build();
        TraceInfo trace = TraceInfo.builder().traceId(UUID.randomUUID().toString()).correlationId(UUID.randomUUID().toString()).build();
        Routing routing = Routing.builder()
                .broker(event.brokerType())
                // Kafka: topic + key(파티션 키)
                .kafka(Routing.Kafka.builder().topic(event.topic()).key(opts.getKafkaKey()).build())
                // Rabbit: exchange + routingKey
                .rabbit(Routing.Rabbit.builder().exchange(event.topic()).routingKey(opts.getRoutingKey()).build())
                .build();
        return Envelope.<CustomEvent>builder()
                .id(UUID.randomUUID().toString())
                .type(event.getClass().getSimpleName())
                .version(event.version())
                .occurredAt(Instant.now())
                .producer(producer)
                .trace(trace)
                .routing(routing)
                .payload(event)
                .build();
    }

    /**
     * Outbox 엔티티를 생성하여 저장합니다.
     * - brokerType에 따라 kafkaTopic 또는 rabbitExchange 중 하나를 채웁니다.
     * - messageKey(=Kafka key), rabbitRoutingKey는 RoutingOptions에서 넘어옵니다.
     * - status는 직렬화 성공 시 PENDING, 실패 시 FAILED로 기록합니다.
     */
    private void persist(CustomEvent event, RoutingOptions opts, String envelopeJson, OutboxEventStatus status, String lastError) {
        Instant now = Instant.now();
        OutboxEventEntity e = OutboxEventEntity.builder()
                .brokerType(event.brokerType())
                .kafkaTopic(event.brokerType() == BrokerType.KAFKA ? event.topic() : null)
                .rabbitExchange(event.brokerType() == BrokerType.RABBIT ? event.topic() : null)
                .rabbitRoutingKey(opts.getRoutingKey())
                .messageKey(opts.getKafkaKey())
                .envelope(envelopeJson)
                .status(status)
                .retryCount(0)
                .lastErrorMessage(lastError)
                .createdAt(now)
                .updatedAt(now)
                .build();
        repository.save(e);
    }
}
