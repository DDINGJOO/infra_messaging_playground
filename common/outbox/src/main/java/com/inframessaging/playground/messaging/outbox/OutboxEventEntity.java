package com.inframessaging.playground.messaging.outbox;

import com.inframessaging.playground.messaging.api.BrokerType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Outbox 테이블(event_outbox)에 적재되는 이벤트 레코드 엔티티입니다.
 * - 메시지 브로커(Kafka/RabbitMQ)로 실제 전송되기 전에 안전하게 DB에 저장합니다.
 * - 전송 성공/실패/재시도/DEAD 전이 상태를 관리합니다.
 * - 직렬화된 Envelope(JSON)를 그대로 저장하여 운영/디버깅 가시성을 확보합니다.
 */
@Entity
@Table(name = "event_outbox")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEventEntity {

    /**
     * Outbox PK (auto increment). 단순 식별자이며 비즈니스 키는 아닙니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 브로커 타입: 해당 이벤트가 어느 브로커로 전송되어야 하는지 명시합니다.
     * - KAFKA: kafkaTopic, messageKey를 사용
     * - RABBIT: rabbitExchange, rabbitRoutingKey를 사용
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "broker_type", nullable = false, length = 16)
    private BrokerType brokerType;

    /**
     * Kafka 전용: 발행할 토픽명. brokerType == KAFKA인 경우에만 값이 채워집니다.
     */
    @Column(name = "kafka_topic")
    private String kafkaTopic;

    /**
     * RabbitMQ 전용: 발행할 익스체인지명. brokerType == RABBIT인 경우에만 값이 채워집니다.
     */
    @Column(name = "rabbit_exchange")
    private String rabbitExchange;

    /**
     * RabbitMQ 전용: 라우팅 키. 익스체인지와 함께 메시지 라우팅을 결정합니다.
     */
    @Column(name = "rabbit_routing_key")
    private String rabbitRoutingKey;

    /**
     * 메시지 키
     * - Kafka: 파티션 키 용도로 사용됩니다(동일 키 메시지 순서 보장 및 파티션 분산에 영향).
     * - RabbitMQ: 일반적으로 사용하지 않으나, 필요 시 식별 목적으로 기록할 수 있습니다.
     */
    @Column(name = "message_key")
    private String messageKey;

    /**
     * 직렬화된 Envelope(JSON) 문자열.
     * - Envelope에는 id, type, version, occurredAt, producer, trace, routing, payload가 포함됩니다.
     * - 전송 시 이 JSON이 그대로 브로커로 송신됩니다.
     */
    @Lob
    @Column(name = "envelope", columnDefinition = "CLOB")
    private String envelope;

    /**
     * Outbox 상태
     * - PENDING: 전송 대기
     * - SENT: 전송 성공(보낸 시각 sentAt 기록)
     * - FAILED: 전송 실패(재시도 대상), nextAttemptAt가 설정될 수 있음
     * - DEAD: 재시도 한도 초과 등으로 사망 처리(옵션: DLQ 라우팅 별도 운영)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxEventStatus status;

    /**
     * 현재까지 실패/재시도 횟수.
     */
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    /**
     * 다음 재시도 예정 시각. Processor는 now >= nextAttemptAt 인 건만 집계합니다.
     */
    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    /**
     * 마지막 에러 메시지(요약). 직렬화 실패/브로커 전송 예외 등의 원인을 저장합니다.
     */
    @Column(name = "last_error_message", length = 1024)
    private String lastErrorMessage;

    /**
     * 레코드 생성 시각(Outbox에 적재된 시각).
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 레코드 마지막 수정 시각(상태 전이/재시도 시 갱신).
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 전송 성공 시 기록되는 시각.
     */
    @Column(name = "sent_at")
    private Instant sentAt;

    /**
     * 낙관적 락 버전. 다중 인스턴스 동시 처리 시 중복 전송을 줄이는 데 도움됩니다.
     */
    @Version
    private Integer version;
}
