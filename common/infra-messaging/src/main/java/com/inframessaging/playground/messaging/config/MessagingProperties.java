package com.inframessaging.playground.messaging.config;

import com.inframessaging.playground.messaging.api.BrokerType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * messaging.* 구성 프로퍼티 바인딩
 * - 브로커 타입(kafka|rabbit)과 트레이싱 전파 여부를 설정합니다.
 */
@Data
@ConfigurationProperties(prefix = "messaging")
public class MessagingProperties {
    /** 브로커 타입(kafka | rabbit) */
    private BrokerType type = BrokerType.KAFKA; // kafka | rabbit

    private Tracing tracing = new Tracing();

    @Data
    public static class Tracing {
        /** 트레이스 헤더 전파 여부 */
        private boolean propagation = true;
    }
}
