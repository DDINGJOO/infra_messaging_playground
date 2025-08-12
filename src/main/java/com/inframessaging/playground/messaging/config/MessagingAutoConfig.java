package com.inframessaging.playground.messaging.config;

import com.inframessaging.playground.messaging.producer.EventProducer;
import com.inframessaging.playground.messaging.producer.LoggingEventProducer;
import com.inframessaging.playground.messaging.outbox.OutboxProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MessagingProperties.class, OutboxProperties.class})
public class MessagingAutoConfig {

    @Bean
    EventProducer eventProducer() {
        // For PoC: provide logging producer
        return new LoggingEventProducer();
    }
}
