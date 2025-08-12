package com.inframessaging.playground.messaging.api;

import lombok.Builder;
import lombok.Value;

/**
 * 발행 시 라우팅 옵션(선택 사항)
 * - Kafka 파티션 키나 Rabbit 라우팅 키를 이벤트 기본값에서 오버라이드할 때 사용합니다.
 */
@Value
@Builder
public class RoutingOptions {
    /** Kafka 파티션 키(예: userId) */
    String kafkaKey;
    /** Rabbit 라우팅 키(예: user.profile.updated) */
    String routingKey;
}
