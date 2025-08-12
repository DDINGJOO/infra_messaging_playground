package com.inframessaging.playground.messaging.outbox;

import com.inframessaging.playground.messaging.api.BrokerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "event_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false, length = 16)
    private BrokerType brokerType;

    @Column(name = "kafka_topic")
    private String kafkaTopic;

    @Column(name = "rabbit_exchange")
    private String rabbitExchange;

    @Column(name = "rabbit_routing_key")
    private String rabbitRoutingKey;

    @Column(name = "message_key")
    private String messageKey;

    @Lob
    @Column(name = "envelope", columnDefinition = "CLOB")
    private String envelope;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error_message", length = 1024)
    private String lastErrorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Version
    private Integer version;
}
