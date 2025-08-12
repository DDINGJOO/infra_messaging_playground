package com.inframessaging.playground.messaging.config;

import com.inframessaging.playground.messaging.producer.EventProducer;
import com.inframessaging.playground.messaging.producer.LoggingEventProducer;
import com.inframessaging.playground.messaging.outbox.OutboxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 메시징/Outbox 관련 프로퍼티 바인딩 및 기본 빈 제공
 * - EventProducer: 실제 브로커 대신 로그 출력으로 동작하는 PoC용 구현체 등록
 */
@Configuration
@EnableConfigurationProperties({MessagingProperties.class, OutboxProperties.class})
public class MessagingAutoConfig {

    /**
     * PoC용 Producer. 실제 전송 대신 로그로 기록합니다.
     */
    @Bean
    EventProducer eventProducer() {
        return new LoggingEventProducer();
    }
}
