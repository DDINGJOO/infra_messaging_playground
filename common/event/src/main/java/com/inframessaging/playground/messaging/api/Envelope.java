package com.inframessaging.playground.messaging.api;

import lombok.*;

import java.time.Instant;

/**
 * Event Envelope (이벤트 표준 래퍼)
 * - 모든 이벤트는 이 Envelope로 감싸져 Outbox에 저장되고 브로커로 전송됩니다.
 * - 공통 메타데이터(id, type, version, 발생시각, 라우팅, 트레이싱 등)를 포함합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Envelope<T> {
    /** 이벤트 ID(ULID/UUID 등). 중복 식별/추적용 */
    String id;
    /** 이벤트 타입(클래스 simpleName 등) */
    String type;
    /** 이벤트 버전(호환성 관리용) */
    int version;
    /** 이벤트 발생 시각(UTC) */
    Instant occurredAt;
    /** 생산자 정보(서비스명/호스트/환경) */
    ProducerInfo producer;
    /** 트레이스 정보(traceId/correlationId) */
    TraceInfo trace;
    /** 라우팅 정보(브로커/토픽/익스체인지/키) */
    Routing routing;
    /** 실제 도메인 이벤트 페이로드 */
    T payload;
}
