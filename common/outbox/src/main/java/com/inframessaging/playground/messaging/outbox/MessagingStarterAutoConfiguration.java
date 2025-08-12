package com.inframessaging.playground.messaging.outbox;

import com.inframessaging.playground.messaging.config.MessagingAutoConfig;
import com.inframessaging.playground.messaging.config.MessagingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

/**
 * Messaging Starter AutoConfiguration (moved to outbox module to avoid circular dependency)
 * - 외부 서비스가 본 모듈을 의존성으로 추가만 해도 자동으로 필요한 빈들이 주입되도록 합니다.
 * - 주요 구성: DomainEventPublisherImpl, OutboxProcessor, Outbox JPA 스캔, Producer 설정(MessagingAutoConfig).
 */
@AutoConfiguration
@EnableConfigurationProperties({MessagingProperties.class, OutboxProperties.class})
@Import({
        // 기본 프로퍼티 바인딩 및 Logging/Real Producer 제공 (infra-messaging 모듈)
        MessagingAutoConfig.class,
        // Outbox 사용을 위한 JPA 스캔(엔티티/리포지토리 활성화)
        OutboxJpaConfig.class,
        // 퍼블리셔/프로세서(스케줄)
        DomainEventPublisherImpl.class,
        OutboxProcessor.class
})
public class MessagingStarterAutoConfiguration {
}
