package com.inframessaging.playground.messaging.api;

import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Envelope<T> {
    String id;            // ULID/UUID for PoC
    String type;          // Event type
    int version;          // Event version
    Instant occurredAt;   // Occurrence time
    ProducerInfo producer;
    TraceInfo trace;
    Routing routing;
    T payload;
}
