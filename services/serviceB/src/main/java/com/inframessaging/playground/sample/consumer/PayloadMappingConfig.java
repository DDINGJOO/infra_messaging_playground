package com.inframessaging.playground.sample.consumer;

import com.inframessaging.playground.messaging.deser.EventPayloadRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Consumer 쪽 payload 매핑 등록 예시
 * - (type="UserRegisteredEvent", version=1) → UserRegisteredPayload.class
 */
@Configuration
@RequiredArgsConstructor
public class PayloadMappingConfig {

    private final EventPayloadRegistry registry;

    @PostConstruct
    public void registerPayloads() {
        registry.register("UserRegisteredEvent", 1, UserRegisteredPayload.class);
    }
}
