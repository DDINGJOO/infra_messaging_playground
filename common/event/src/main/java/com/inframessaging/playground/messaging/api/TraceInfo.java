package com.inframessaging.playground.messaging.api;

import lombok.Builder;
import lombok.Value;

/**
 * 트레이스/상관관계 식별자
 * - traceId: 트랜잭션 전반을 따라가는 ID
 * - correlationId: 관련 이벤트/요청 간 상관관계를 표현하는 ID
 */
@Value
@Builder
public class TraceInfo {
    String traceId;
    String correlationId;
}
