package com.inframessaging.playground.messaging.api;

import lombok.Builder;
import lombok.Value;

/**
 * 이벤트 생산자(발행 주체) 정보
 * - 서비스명, 호스트, 환경명 등을 기록하여 추적성을 높입니다.
 */
@Value
@Builder
public class ProducerInfo {
    /** 서비스 이름(예: user-profile-service) */
    String service;
    /** 호스트명 또는 인스턴스 식별자 */
    String host;
    /** 환경 정보(예: local/dev/stage/prod) */
    String env;
}
