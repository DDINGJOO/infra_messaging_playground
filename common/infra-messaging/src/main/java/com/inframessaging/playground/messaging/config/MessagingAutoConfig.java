package com.inframessaging.playground.messaging.config;

import com.inframessaging.playground.messaging.producer.EventProducer;
import com.inframessaging.playground.messaging.producer.LoggingEventProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 메시징 Producer 관련 기본 빈 제공
 * - EventProducer: 설정에 따라 mock(logging) 또는 real(Kafka/RabbitTemplate) 구현을 노출합니다.
 */
@Configuration
@EnableConfigurationProperties({MessagingProperties.class})
public class MessagingAutoConfig {

    /**
     * producer=real 일 때 실제 Kafka/RabbitTemplate 기반 프로듀서 등록
     */
    @Bean
    @ConditionalOnProperty(prefix = "messaging", name = "producer", havingValue = "real")
    @ConditionalOnMissingBean(EventProducer.class)
    public EventProducer realEventProducer(MessagingProperties props,
                                           org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate,
                                           org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
        return new com.inframessaging.playground.messaging.producer.RealEventProducer(kafkaTemplate, rabbitTemplate);
    }

    /**
     * 기본(mock) 프로듀서: 브로커 없이 로그만 출력
     */
    @Bean
    @ConditionalOnProperty(prefix = "messaging", name = "producer", havingValue = "mock", matchIfMissing = true)
    @ConditionalOnMissingBean(EventProducer.class)
    public EventProducer loggingEventProducer(MessagingProperties props) {
        return new LoggingEventProducer();
    }
}
