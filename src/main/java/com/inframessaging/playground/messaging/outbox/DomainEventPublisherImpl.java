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

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainEventPublisherImpl implements DomainEventPublisher {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(CustomEvent event) {
        publish(event, RoutingOptions.builder().build());
    }

    @Override
    public void publish(CustomEvent event, RoutingOptions opts) {
        Envelope<CustomEvent> envelope = buildEnvelope(event, opts);
        String json;
        try {
            json = objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException e) {
            log.error("Envelope serialization failed", e);
            // still persist as FAILED for visibility
            persist(event, opts, null, OutboxEventStatus.FAILED, e.getMessage());
            return;
        }
        persist(event, opts, json, OutboxEventStatus.PENDING, null);
    }

    private Envelope<CustomEvent> buildEnvelope(CustomEvent event, RoutingOptions opts) {
        String host = "unknown";
        try { host = InetAddress.getLocalHost().getHostName(); } catch (UnknownHostException ignored) {}
        ProducerInfo producer = ProducerInfo.builder().service("infra-messaging-playground").host(host).env("local").build();
        TraceInfo trace = TraceInfo.builder().traceId(UUID.randomUUID().toString()).correlationId(UUID.randomUUID().toString()).build();
        Routing routing = Routing.builder()
                .broker(event.brokerType())
                .kafka(Routing.Kafka.builder().topic(event.topic()).key(opts.getKafkaKey()).build())
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
