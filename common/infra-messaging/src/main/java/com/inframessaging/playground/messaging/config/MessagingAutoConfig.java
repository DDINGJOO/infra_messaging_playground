package com.inframessaging.playground.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inframessaging.playground.messaging.deser.EnvelopeDeserializer;
import com.inframessaging.playground.messaging.deser.EventPayloadRegistry;
import com.inframessaging.playground.messaging.producer.EventProducer;
import com.inframessaging.playground.messaging.producer.RealEventProducer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 메시징 Producer 관련 기본 빈 제공
 * - 항상 실제 Kafka/RabbitTemplate 기반 프로듀서를 노출합니다.
 */
@Configuration
@EnableConfigurationProperties({MessagingProperties.class})
public class MessagingAutoConfig {

    /**
     * ObjectMapper 기본 빈 (없을 경우에만)
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    /**
     * 실제 Kafka/RabbitTemplate 기반 프로듀서 등록 (기본)
     */
    @Bean
    @ConditionalOnMissingBean(EventProducer.class)
    public EventProducer eventProducer(org.springframework.kafka.core.KafkaTemplate<String, String> kafkaTemplate,
                                       org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate) {
        return new RealEventProducer(kafkaTemplate, rabbitTemplate);
    }

    /**
     * payload 타입 매핑 레지스트리
     */
    @Bean
    @ConditionalOnMissingBean(EventPayloadRegistry.class)
    public EventPayloadRegistry eventPayloadRegistry() {
        return new EventPayloadRegistry();
    }

    /**
     * Envelope 역직렬화 유틸리티
     */
    @Bean
    @ConditionalOnMissingBean(EnvelopeDeserializer.class)
    public EnvelopeDeserializer envelopeDeserializer(ObjectMapper objectMapper, EventPayloadRegistry registry) {
        return new EnvelopeDeserializer(objectMapper, registry);
    }
}
